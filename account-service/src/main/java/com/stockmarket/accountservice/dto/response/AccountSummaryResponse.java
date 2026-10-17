package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Financial overview of an account.
 *
 * Market values use live prices; when market-data-service is unavailable the
 * average purchase price is used and {@code pricesLive} is false.
 */
@Schema(description = "Financial overview of an account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryResponse {

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Account number.
     */
    private String accountNumber;

    /**
     * Display name.
     */
    private String accountName;

    /**
     * Currency.
     */
    private String currency;

    /**
     * Status.
     */
    private AccountStatus status;

    /**
     * Cash available.
     */
    private BigDecimal cashBalance;

    /**
     * Number of symbols with shares held.
     */
    private int openPositions;

    /**
     * Cost basis of all shares held.
     */
    private BigDecimal investedAmount;

    /**
     * Current value of all shares held.
     */
    private BigDecimal marketValue;

    /**
     * marketValue - investedAmount.
     */
    private BigDecimal unrealizedProfit;

    /**
     * Sum of realized profit from SELL trades.
     */
    private BigDecimal realizedProfit;

    /**
     * cashBalance + marketValue.
     */
    private BigDecimal totalEquity;

    /**
     * All completed deposits.
     */
    private BigDecimal totalDeposits;

    /**
     * All completed withdrawals (excluding fees).
     */
    private BigDecimal totalWithdrawals;

    /**
     * All fees charged.
     */
    private BigDecimal totalFees;

    /**
     * All taxes charged.
     */
    private BigDecimal totalTaxes;

    /**
     * Net dividends received.
     */
    private BigDecimal totalDividends;

    /**
     * Interest received.
     */
    private BigDecimal totalInterest;

    /**
     * False when live prices could not be fetched for every holding.
     */
    private boolean pricesLive;
}
