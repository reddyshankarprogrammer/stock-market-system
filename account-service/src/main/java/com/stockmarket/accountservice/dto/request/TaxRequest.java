package com.stockmarket.accountservice.dto.request;

import com.stockmarket.accountservice.enums.TradeSide;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Asks the tax engine for a tax preview without charging anything.
 */
@Schema(description = "Asks the tax engine for a tax preview without charging anything")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRequest {

    /**
     * BUY or SELL.
     */
    @NotNull(message = "Side is required")
    private TradeSide side;

    /**
     * Gross trade value.
     */
    @NotNull(message = "Trade value is required")
    @DecimalMin(value = "0.01", message = "Trade value must be at least 0.01")
    @Digits(integer = 15, fraction = 2, message = "Trade value must have at most 2 decimal places")
    @Schema(example = "1953.00")
    private BigDecimal tradeValue;

    /**
     * SELL only: realized profit (negative for a loss) for capital gains tax.
     */
    @Digits(integer = 15, fraction = 2, message = "Realized profit must have at most 2 decimal places")
    @Schema(example = "120.50")
    private BigDecimal realizedProfit;
}
