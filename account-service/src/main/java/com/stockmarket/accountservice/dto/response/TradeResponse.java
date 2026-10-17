package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeSide;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A trade: executed here, entered manually, or imported from a statement.
 */
@Schema(description = "A trade: executed here, entered manually, or imported from a statement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    /**
     * Trade id.
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
     * Exchange or venue.
     */
    private String exchange;

    /**
     * BUY or SELL (for a closed position: BUY = long, SELL = short).
     */
    private TradeSide side;

    /**
     * EXECUTION or CLOSED_POSITION.
     */
    private TradeKind tradeKind;

    /**
     * PLATFORM, MANUAL or IMPORT.
     */
    private RecordSource source;

    /**
     * Units traded (fractional for crypto and forex).
     */
    private BigDecimal quantity;

    /**
     * Execution price (closing price for a closed position).
     */
    private BigDecimal price;

    /**
     * Purchase price.
     */
    private BigDecimal buyPrice;

    /**
     * Sale price.
     */
    private BigDecimal sellPrice;

    /**
     * price x quantity.
     */
    private BigDecimal grossAmount;

    /**
     * Broker commission.
     */
    private BigDecimal brokerage;

    /**
     * Other fees.
     */
    private BigDecimal otherFee;

    /**
     * Total fees: brokerage + otherFee.
     */
    private BigDecimal feeAmount;

    /**
     * Total taxes.
     */
    private BigDecimal taxAmount;

    /**
     * BUY: total debited. SELL: total credited.
     */
    private BigDecimal netAmount;

    /**
     * SELL and closed positions: realized profit after fees, before taxes.
     */
    private BigDecimal realizedProfit;

    /**
     * SELL: average cost of the units sold; closed position: buy price.
     */
    private BigDecimal averageCostAtTrade;

    /**
     * PLATFORM trades: account balance after the trade; set on the trade response only.
     */
    private BigDecimal balanceAfter;

    /**
     * Execution time.
     */
    private LocalDateTime tradeDate;

    /**
     * Statement it was imported from.
     */
    private String statementId;

    /**
     * Broker's trade or ticket id.
     */
    private String externalTradeId;

    /**
     * Notes.
     */
    private String notes;
}
