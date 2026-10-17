package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.request.AdjustmentRequest;
import com.stockmarket.accountservice.dto.request.DepositRequest;
import com.stockmarket.accountservice.dto.request.WithdrawRequest;
import com.stockmarket.accountservice.dto.response.FundTransactionResponse;
import com.stockmarket.accountservice.entity.AccountFundTransaction;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.AuditAction;
import com.stockmarket.accountservice.enums.FundTransactionType;
import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.ReferenceType;
import com.stockmarket.accountservice.enums.TransactionStatus;
import com.stockmarket.accountservice.event.FundTransactionEvent;
import com.stockmarket.accountservice.exception.InsufficientBalanceException;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.FundTransactionRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.AuditService;
import com.stockmarket.accountservice.service.FeeCalculationService;
import com.stockmarket.accountservice.service.FundService;
import com.stockmarket.accountservice.service.LedgerService;
import com.stockmarket.accountservice.service.model.FeeBreakdown;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundServiceImpl implements FundService {

    private final FundTransactionRepository fundTransactionRepository;
    private final AccountAccessService accountAccessService;
    private final LedgerService ledgerService;
    private final FeeCalculationService feeCalculationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountMapper mapper;

    @Override
    @Transactional
    public FundTransactionResponse deposit(String accountId, String userId, DepositRequest request) {

        InvestmentAccount account = accountAccessService.getOwnedActiveAccountForUpdate(accountId, userId);
        BigDecimal amount = Money.amount(request.getAmount());

        AccountFundTransaction transaction = fundTransactionRepository.save(newTransaction(
                account, FundTransactionType.DEPOSIT, amount, Money.zero(), request.getReference()));

        ledgerService.postCredit(account, amount, LedgerCategory.DEPOSIT,
                ReferenceType.FUND_TRANSACTION, transaction.getId(), describe("Deposit", request.getReference()));

        return complete(account, transaction, AuditAction.DEPOSIT, "Deposited " + amount.toPlainString());
    }

    @Override
    @Transactional
    public FundTransactionResponse withdraw(String accountId, String userId, WithdrawRequest request) {

        InvestmentAccount account = accountAccessService.getOwnedActiveAccountForUpdate(accountId, userId);
        BigDecimal amount = Money.amount(request.getAmount());
        FeeBreakdown fees = feeCalculationService.calculateWithdrawalFees(amount);
        BigDecimal totalDebit = amount.add(fees.total());

        // Check the whole debit up front so the request fails cleanly instead of half-way.
        if (account.getBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientBalanceException(totalDebit, account.getBalance());
        }

        AccountFundTransaction transaction = fundTransactionRepository.save(newTransaction(
                account, FundTransactionType.WITHDRAWAL, amount, fees.total(), request.getReference()));

        ledgerService.postDebit(account, amount, LedgerCategory.WITHDRAWAL,
                ReferenceType.FUND_TRANSACTION, transaction.getId(), describe("Withdrawal", request.getReference()));

        if (fees.total().signum() > 0) {
            ledgerService.postDebit(account, fees.total(), LedgerCategory.FEE,
                    ReferenceType.FUND_TRANSACTION, transaction.getId(), "Withdrawal fee");
            feeCalculationService.recordCharges(account, fees, null, transaction);
        }

        return complete(account, transaction, AuditAction.WITHDRAW,
                "Withdrew " + amount.toPlainString() + " (fee " + fees.total().toPlainString() + ")");
    }

    @Override
    @Transactional
    public FundTransactionResponse adjust(String accountId, String userId, AdjustmentRequest request) {

        if (request.getAmount().signum() == 0) {
            throw new ValidationException("Adjustment amount must not be zero");
        }
        InvestmentAccount account = accountAccessService.getOwnedActiveAccountForUpdate(accountId, userId);

        boolean credit = request.getAmount().signum() > 0;
        BigDecimal amount = Money.amount(request.getAmount().abs());
        String reference = request.getStatementId() != null
                ? "STATEMENT:" + request.getStatementId()
                : null;

        AccountFundTransaction transaction = fundTransactionRepository.save(newTransaction(account,
                credit ? FundTransactionType.ADJUSTMENT_IN : FundTransactionType.ADJUSTMENT_OUT,
                amount, Money.zero(), reference));

        String description = "Adjustment: " + request.getDescription();
        if (credit) {
            ledgerService.postCredit(account, amount, LedgerCategory.STATEMENT_ADJUSTMENT,
                    ReferenceType.FUND_TRANSACTION, transaction.getId(), description);
        } else {
            ledgerService.postDebit(account, amount, LedgerCategory.STATEMENT_ADJUSTMENT,
                    ReferenceType.FUND_TRANSACTION, transaction.getId(), description);
        }

        return complete(account, transaction, AuditAction.ADJUSTMENT,
                (credit ? "+" : "-") + amount.toPlainString() + " " + request.getDescription());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundTransactionResponse> getFundTransactions(String accountId, String userId) {

        accountAccessService.getOwnedAccount(accountId, userId);
        return mapper.toFundTransactionResponses(
                fundTransactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId));
    }

    private AccountFundTransaction newTransaction(InvestmentAccount account, FundTransactionType type,
                                                  BigDecimal amount, BigDecimal fee, String reference) {
        return AccountFundTransaction.builder()
                .account(account)
                .transactionType(type)
                .amount(amount)
                .feeAmount(fee)
                // Replaced with the real balance once the ledger entries are posted.
                .balanceAfter(account.getBalance())
                .reference(reference)
                .status(TransactionStatus.COMPLETED)
                .transactionDate(LocalDateTime.now())
                .build();
    }

    private FundTransactionResponse complete(InvestmentAccount account, AccountFundTransaction transaction,
                                             AuditAction action, String details) {

        transaction.setBalanceAfter(account.getBalance());

        auditService.logAccountAction(account.getId(), action, details);
        eventPublisher.publishEvent(new FundTransactionEvent(
                transaction.getId(),
                account.getId(),
                account.getUserId(),
                transaction.getTransactionType().name(),
                transaction.getAmount(),
                transaction.getFeeAmount(),
                account.getBalance(),
                transaction.getTransactionDate()));

        log.info("{} on account {}: {} -> balance {}", transaction.getTransactionType(),
                account.getAccountNumber(), transaction.getAmount(), account.getBalance());

        return mapper.toFundTransactionResponse(transaction);
    }

    private static String describe(String label, String reference) {
        return reference == null || reference.isBlank() ? label : label + " (" + reference + ")";
    }
}
