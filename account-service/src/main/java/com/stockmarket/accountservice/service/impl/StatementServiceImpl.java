package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.response.StatementResponse;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountFeeRepository;
import com.stockmarket.accountservice.repository.AccountTaxRepository;
import com.stockmarket.accountservice.repository.DividendRepository;
import com.stockmarket.accountservice.repository.FundTransactionRepository;
import com.stockmarket.accountservice.repository.InterestRepository;
import com.stockmarket.accountservice.repository.TradeRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.LedgerService;
import com.stockmarket.accountservice.service.StatementService;
import com.stockmarket.accountservice.service.model.LedgerStatement;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class StatementServiceImpl implements StatementService {

    /**
     * Longest statement period, to keep responses bounded.
     */
    private static final long MAX_PERIOD_DAYS = 366;

    private final AccountAccessService accountAccessService;
    private final LedgerService ledgerService;
    private final TradeRepository tradeRepository;
    private final FundTransactionRepository fundTransactionRepository;
    private final DividendRepository dividendRepository;
    private final InterestRepository interestRepository;
    private final AccountFeeRepository accountFeeRepository;
    private final AccountTaxRepository accountTaxRepository;
    private final AccountMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public StatementResponse generateStatement(String accountId, String userId, LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new ValidationException("'from' must not be after 'to'");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_PERIOD_DAYS) {
            throw new ValidationException("A statement can cover at most " + MAX_PERIOD_DAYS + " days");
        }

        InvestmentAccount account = accountAccessService.getOwnedAccount(accountId, userId);

        // Half-open [start, end) for the ledger; inclusive bounds for the BETWEEN queries.
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        LocalDateTime lastMicro = end.minusNanos(1_000);

        LedgerStatement ledger = ledgerService.generateRunningBalance(accountId, start, end);

        return StatementResponse.builder()
                .account(mapper.toAccountResponse(account))
                .from(from)
                .to(to)
                .openingBalance(ledger.openingBalance())
                .totalCredits(ledger.totalCredits())
                .totalDebits(ledger.totalDebits())
                .closingBalance(ledger.closingBalance())
                .totalFees(Money.amount(accountFeeRepository.sumFeesByAccountIdForPeriod(accountId, start, end)))
                .totalTaxes(Money.amount(accountTaxRepository.sumTaxesByAccountIdForPeriod(accountId, start, end)))
                .ledgerEntries(mapper.toLedgerResponses(ledger.entries()))
                .trades(mapper.toTradeResponses(
                        tradeRepository.findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, start, lastMicro)))
                .fundTransactions(mapper.toFundTransactionResponses(
                        fundTransactionRepository.findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                                accountId, start, lastMicro)))
                .dividends(mapper.toDividendResponses(
                        dividendRepository.findByAccountIdAndPaymentDateBetweenOrderByPaymentDateAsc(accountId, from, to)))
                .interest(mapper.toInterestResponses(
                        interestRepository.findByAccountIdAndPostedAtBetweenOrderByPostedAtAsc(accountId, start, lastMicro)))
                .build();
    }
}
