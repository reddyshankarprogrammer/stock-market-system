package com.stockmarket.accountservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Buys shares at the current market price.
 */
@Schema(description = "Buys shares at the current market price")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyTradeRequest {

    /**
     * Account to trade on; must belong to the caller.
     */
    @NotBlank(message = "Account id is required")
    private String accountId;

    /**
     * Stock symbol; case-insensitive.
     */
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "[A-Za-z.]{1,20}", message = "Symbol must be 1-20 letters")
    @Schema(example = "AAPL")
    private String symbol;

    /**
     * Number of shares.
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000000, message = "Quantity must be at most 1,000,000")
    @Schema(example = "10")
    private Integer quantity;
}
