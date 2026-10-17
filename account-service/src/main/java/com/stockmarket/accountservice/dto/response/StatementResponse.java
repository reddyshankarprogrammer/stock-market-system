package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Account statement for a period.
 *
 * The period is [from, to] in whole days. {@code closingBalance} equals
 * {@code openingBalance + totalCredits - totalDebits}.
 */
@Schema(description = "Account statement for a period")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementResponse {

    /**
     * The account.
     */
    private AccountResponse account;

    /**
     * First day (inclusive).
     */
    private LocalDate from;

    /**
     * Last day (inclusive).
     */
    private LocalDate to;

    /**
     * Balance at the start of the period.
     */
    private BigDecimal openingBalance;

    /**
     * Sum of credits in the period.
     */
    private BigDecimal totalCredits;

    /**
     * Sum of debits in the period.
     */
    private BigDecimal totalDebits;

    /**
     * Balance at the end of the period.
     */
    private BigDecimal closingBalance;

    /**
     * Fees charged in the period.
     */
    private BigDecimal totalFees;

    /**
     * Taxes charged in the period.
     */
    private BigDecimal totalTaxes;

    /**
     * Ledger entries with running balance, oldest first.
     */
    private List<LedgerResponse> ledgerEntries;

    /**
     * Trades in the period.
     */
    private List<TradeResponse> trades;

    /**
     * Deposits and withdrawals in the period.
     */
    private List<FundTransactionResponse> fundTransactions;

    /**
     * Dividends paid in the period.
     */
    private List<DividendResponse> dividends;

    /**
     * Interest posted in the period.
     */
    private List<InterestResponse> interest;
}
