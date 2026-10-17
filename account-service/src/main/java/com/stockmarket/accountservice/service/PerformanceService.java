package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.response.PerformanceResponse;

/**
 * Account performance.
 *
 * <pre>
 * realizedProfit   = sum of trade realized profit (after the trade's own fees, before taxes)
 * unrealizedProfit = open positions at the live price (or last known / cost price)
 * grossProfit      = realizedProfit + unrealizedProfit + the fees deducted inside realizedProfit
 * totalCharges     = platform fees (account_fees) + recorded charges (account_charges)
 * totalTaxes       = all account_taxes
 * netProfit        = grossProfit - totalCharges - totalTaxes
 * </pre>
 */
public interface PerformanceService {

    /**
     * Recalculates the account's performance, stores the snapshot and returns it.
     */
    PerformanceResponse calculatePerformance(String accountId, String userId);
}
