package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.BrokerName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Account performance.
 *
 * grossProfit = realizedProfit + unrealizedProfit + fees already deducted from realized profit;
 * netProfit = grossProfit - totalCharges - totalTaxes.
 */
@Schema(description = "Account performance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceResponse {

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Owner.
     */
    private String userId;

    /**
     * Broker.
     */
    private BrokerName brokerName;

    /**
     * Profit before charges and taxes.
     */
    private BigDecimal grossProfit;

    /**
     * Realized profit after trade fees.
     */
    private BigDecimal realizedProfit;

    /**
     * Open positions at current (or last known) prices.
     */
    private BigDecimal unrealizedProfit;

    /**
     * All charges and fees.
     */
    private BigDecimal totalCharges;

    /**
     * All taxes.
     */
    private BigDecimal totalTaxes;

    /**
     * grossProfit - totalCharges - totalTaxes.
     */
    private BigDecimal netProfit;

    /**
     * False when some holdings used last known or cost prices.
     */
    private boolean pricesLive;

    /**
     * When it was calculated.
     */
    private LocalDateTime calculatedAt;
}
