package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.response.AccountChargeResponse;
import com.stockmarket.accountservice.dto.response.ChargeSummaryResponse;

import java.util.List;

/**
 * Charges recorded from statements and manual trades, and charge summaries that also
 * include the platform fee engine's fees.
 */
public interface ChargeService {

    List<AccountChargeResponse> getCharges(String accountId, String userId);

    /**
     * Totals per charge type for a calendar year. Platform fees are mapped onto charge types:
     * BROKERAGE -> BROKERAGE, EXCHANGE -> EXCHANGE_FEE, PLATFORM and WITHDRAWAL -> OTHER.
     */
    ChargeSummaryResponse getChargeSummary(String accountId, String userId, int year);
}
