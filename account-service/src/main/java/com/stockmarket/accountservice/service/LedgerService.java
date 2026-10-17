package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.response.LedgerResponse;
import com.stockmarket.accountservice.entity.AccountLedger;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.ReferenceType;
import com.stockmarket.accountservice.exception.InsufficientBalanceException;
import com.stockmarket.accountservice.service.model.LedgerStatement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The only component that changes an account balance.
 *
 * Every debit or credit updates {@code InvestmentAccount.balance} and appends a ledger
 * entry carrying the new running balance, in the caller's transaction. Callers must
 * hold the account row lock ({@code AccountAccessService.*ForUpdate}).
 */
public interface LedgerService {

    /**
     * Reduces the balance.
     *
     * @throws InsufficientBalanceException if the balance is lower than {@code amount}
     */
    AccountLedger postDebit(InvestmentAccount account, BigDecimal amount, LedgerCategory category,
                            ReferenceType referenceType, String referenceId, String description);

    /**
     * Increases the balance.
     */
    AccountLedger postCredit(InvestmentAccount account, BigDecimal amount, LedgerCategory category,
                             ReferenceType referenceType, String referenceId, String description);

    /**
     * Recomputes the running balance over [from, to) starting from the balance before
     * {@code from}, and checks it against each entry's stored {@code balanceAfter}.
     */
    LedgerStatement generateRunningBalance(String accountId, LocalDateTime from, LocalDateTime to);

    /**
     * Ledger of an account owned by the caller for the inclusive day range.
     */
    List<LedgerResponse> getLedger(String accountId, String userId, LocalDate from, LocalDate to);
}
