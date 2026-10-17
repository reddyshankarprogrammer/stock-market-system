package com.stockmarket.accountservice.service.model;

import com.stockmarket.accountservice.entity.AccountLedger;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ledger entries of a period together with the balances around them.
 *
 * @param openingBalance balance before the first entry of the period
 * @param totalCredits   sum of CREDIT entries
 * @param totalDebits    sum of DEBIT entries
 * @param closingBalance openingBalance + totalCredits - totalDebits
 * @param entries        entries oldest first; each {@code balanceAfter} is the running balance
 */
public record LedgerStatement(BigDecimal openingBalance,
                              BigDecimal totalCredits,
                              BigDecimal totalDebits,
                              BigDecimal closingBalance,
                              List<AccountLedger> entries) {
}
