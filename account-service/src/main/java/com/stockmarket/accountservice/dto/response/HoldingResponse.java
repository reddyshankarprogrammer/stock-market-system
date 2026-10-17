package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shares of one symbol held in an account, with live valuation.
 */
@Schema(description = "Shares of one symbol held in an account, with live valuation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingResponse {

    /**
     * Holding id.
     */
    private String id;

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Owner.
     */
    private String userId;

    /**
     * Symbol.
     */
    private String symbol;

    /**
     * Units held (fractional for crypto and forex).
     */
    private BigDecimal quantity;

    /**
     * Weighted average purchase price.
     */
    private BigDecimal averagePrice;

    /**
     * Cost basis of the units held.
     */
    private BigDecimal totalCost;

    /**
     * Same as totalCost: the amount invested in the units held.
     */
    private BigDecimal investedAmount;

    /**
     * Cumulative realized profit for this symbol.
     */
    private BigDecimal realizedProfit;

    /**
     * Live price; null when market data is unavailable.
     */
    private BigDecimal currentPrice;

    /**
     * currentPrice x quantity; null when market data is unavailable.
     */
    private BigDecimal marketValue;

    /**
     * marketValue - totalCost; null when market data is unavailable.
     */
    private BigDecimal unrealizedProfit;

    /**
     * unrealizedProfit as a percentage of totalCost.
     */
    private BigDecimal unrealizedProfitPercent;

    /**
     * When currentPrice was last refreshed.
     */
    private LocalDateTime priceUpdatedAt;
}
