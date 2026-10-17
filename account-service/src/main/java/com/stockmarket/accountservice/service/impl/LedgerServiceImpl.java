package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.response.LedgerResponse;
import com.stockmarket.accountservice.entity.AccountLedger;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.LedgerEntryType;
import com.stockmarket.accountservice.enums.ReferenceType;
import com.stockmarket.accountservice.exception.InsufficientBalanceException;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountLedgerRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.LedgerService;
import com.stockmarket.accountservice.service.model.LedgerStatement;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final AccountLedgerRepository ledgerRepository;
    private final AccountAccessService accountAccessService;
    private final AccountMapper mapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AccountLedger postDebit(InvestmentAccount account, BigDecimal amount, LedgerCategory category,
                                   ReferenceType referenceType, String referenceId, String description) {

        BigDecimal value = requirePositive(amount);

        if (account.getBalance().compareTo(value) < 0) {
            throw new InsufficientBalanceException(value, account.getBalance());
        }

        account.setBalance(Money.amount(account.getBalance().subtract(value)));
        return append(account, LedgerEntryType.DEBIT, value, category, referenceType, referenceId, description);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AccountLedger postCredit(InvestmentAccount account, BigDecimal amount, LedgerCategory category,
                                    ReferenceType referenceType, String referenceId, String description) {

        BigDecimal value = requirePositive(amount);

        account.setBalance(Money.amount(account.getBalance().add(value)));
        return append(account, LedgerEntryType.CREDIT, value, category, referenceType, referenceId, description);
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerStatement generateRunningBalance(String accountId, LocalDateTime from, LocalDateTime to) {

        BigDecimal openingBalance = ledgerRepository.findLatestBefore(accountId, from, Limit.of(1)).stream()
                .findFirst()
                .map(AccountLedger::getBalanceAfter)
                .orElse(Money.zero());

        List<AccountLedger> entries = ledgerRepository.findForPeriod(accountId, from, to);

        BigDecimal running = openingBalance;
        BigDecimal credits = Money.zero();
        BigDecimal debits = Money.zero();

        for (AccountLedger entry : entries) {
            if (entry.getEntryType() == LedgerEntryType.CREDIT) {
                running = running.add(entry.getAmount());
                credits = credits.add(entry.getAmount());
            } else {
                running = running.subtract(entry.getAmount());
                debits = debits.add(entry.getAmount());
            }

            // The stored running balance must match the recomputed one; a mismatch means
            // the balance was changed outside LedgerService.
            if (running.compareTo(entry.getBalanceAfter()) != 0) {
                log.error("Ledger mismatch on account {} entry #{}: stored {}, recomputed {}",
                        accountId, entry.getEntryNumber(), entry.getBalanceAfter(), running);
            }
        }

        return new LedgerStatement(openingBalance, credits, debits, Money.amount(running), entries);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerResponse> getLedger(String accountId, String userId, LocalDate from, LocalDate to) {

        accountAccessService.getOwnedAccount(accountId, userId);
        if (from.isAfter(to)) {
            throw new ValidationException("'from' must not be after 'to'");
        }

        LedgerStatement statement =
                generateRunningBalance(accountId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return mapper.toLedgerResponses(statement.entries());
    }

    /**
     * Appends an entry with the next entry number. The caller holds the account row lock,
     * so MAX + 1 cannot race; the (account_id, entry_number) unique key is the backstop.
     */
    private AccountLedger append(InvestmentAccount account, LedgerEntryType type, BigDecimal amount,
                                 LedgerCategory category, ReferenceType referenceType,
                                 String referenceId, String description) {

        long entryNumber = ledgerRepository.findMaxEntryNumber(account.getId()) + 1;

        AccountLedger entry = AccountLedger.builder()
                .account(account)
                .entryNumber(entryNumber)
                .entryType(type)
                .category(category)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description(description)
                .transactionDate(LocalDateTime.now())
                .build();

        AccountLedger saved = ledgerRepository.save(entry);

        log.debug("Ledger #{} {} {} {} on account {} -> balance {}",
                entryNumber, type, category, amount, account.getId(), account.getBalance());
        return saved;
    }

    private BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Ledger amount must be positive: " + amount);
        }
        return Money.amount(amount);
    }
}
