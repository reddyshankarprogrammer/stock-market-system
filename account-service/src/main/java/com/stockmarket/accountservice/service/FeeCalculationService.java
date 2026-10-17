package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.request.FeeRequest;
import com.stockmarket.accountservice.dto.response.AccountFeeResponse;
import com.stockmarket.accountservice.dto.response.FeeBreakdownResponse;
import com.stockmarket.accountservice.dto.response.FeeMasterResponse;
import com.stockmarket.accountservice.entity.AccountFee;
import com.stockmarket.accountservice.entity.AccountFundTransaction;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.service.model.FeeBreakdown;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fee engine driven by the active {@code fee_master} rows.
 *
 * Each fee is FIXED (the configured value) or PERCENTAGE (value% of the base amount),
 * then clamped to the optional minimum and maximum and rounded to 2 decimals.
 * A fee type without an active row costs zero.
 */
public interface FeeCalculationService {

    BigDecimal calculateBrokerage(BigDecimal tradeValue);

    BigDecimal calculatePlatformFee(BigDecimal tradeValue);

    BigDecimal calculateExchangeFee(BigDecimal tradeValue);

    BigDecimal calculateWithdrawalFee(BigDecimal amount);

    /**
     * Brokerage + platform + exchange fees for a trade.
     */
    FeeBreakdown calculateTotalFees(BigDecimal tradeValue);

    /**
     * Withdrawal fee as a breakdown, for recording.
     */
    FeeBreakdown calculateWithdrawalFees(BigDecimal amount);

    /**
     * Persists one {@code AccountFee} per charge, linked to the trade or withdrawal.
     */
    List<AccountFee> recordCharges(InvestmentAccount account, FeeBreakdown fees,
                                   Trade trade, AccountFundTransaction fundTransaction);

    FeeBreakdownResponse previewFees(FeeRequest request);

    List<FeeMasterResponse> getFeeSchedule();

    List<AccountFeeResponse> getAccountFees(String accountId, String userId);
}
