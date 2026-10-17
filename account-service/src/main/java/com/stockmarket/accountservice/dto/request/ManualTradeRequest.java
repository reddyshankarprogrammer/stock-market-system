package com.stockmarket.accountservice.dto.request;

import com.stockmarket.accountservice.enums.TradeSide;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a trade done at an external broker on a tracking account. Never moves cash.
 *
 * Give buyPrice for a BUY, sellPrice for a SELL, or both for a closed position
 * (side BUY = long, SELL = short).
 */
@Schema(description = "Records a trade done at an external broker on a tracking account. Never moves cash")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualTradeRequest {

    /**
     * Tracking account; must belong to the caller.
     */
    @NotBlank(message = "Account id is required")
    private String accountId;

    /**
     * Symbol; stored upper-case.
     */
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "[A-Za-z.]{1,20}", message = "Symbol must be 1-20 letters")
    @Schema(example = "AAPL")
    private String symbol;

    /**
     * Exchange or venue.
     */
    @Size(max = 20, message = "Exchange must be at most 20 characters")
    @Schema(example = "NSE")
    private String exchange;

    /**
     * BUY or SELL.
     */
    @NotNull(message = "Side is required")
    private TradeSide side;

    /**
     * Units traded.
     */
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be positive")
    @Digits(integer = 11, fraction = 8, message = "Quantity must have at most 8 decimal places")
    @Schema(example = "10")
    private BigDecimal quantity;

    /**
     * Purchase price.
     */
    @DecimalMin(value = "0.00000001", message = "Buy price must be positive")
    @Digits(integer = 11, fraction = 8, message = "Buy price must have at most 8 decimal places")
    @Schema(example = "2450.50")
    private BigDecimal buyPrice;

    /**
     * Sale price.
     */
    @DecimalMin(value = "0.00000001", message = "Sell price must be positive")
    @Digits(integer = 11, fraction = 8, message = "Sell price must have at most 8 decimal places")
    private BigDecimal sellPrice;

    /**
     * Broker commission.
     */
    @DecimalMin(value = "0.00", message = "Brokerage must not be negative")
    private BigDecimal brokerage;

    /**
     * Other fees.
     */
    @DecimalMin(value = "0.00", message = "Fee must not be negative")
    private BigDecimal fee;

    /**
     * Taxes on the trade.
     */
    @DecimalMin(value = "0.00", message = "Tax must not be negative")
    private BigDecimal tax;

    /**
     * When the trade happened.
     */
    @NotNull(message = "Trade date is required")
    @PastOrPresent(message = "Trade date must not be in the future")
    private LocalDateTime tradeDate;

    /**
     * Broker's trade id; prevents recording the same trade twice.
     */
    @Size(max = 100, message = "External trade id must be at most 100 characters")
    private String externalTradeId;

    /**
     * Notes.
     */
    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;
}
