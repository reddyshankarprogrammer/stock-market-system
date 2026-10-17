package com.stockmarket.accountservice.dto.request;

import com.stockmarket.accountservice.enums.FeeCalculationKind;
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
 * Asks the fee engine for a fee preview without charging anything.
 */
@Schema(description = "Asks the fee engine for a fee preview without charging anything")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeRequest {

    /**
     * Trade value or withdrawal amount.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(example = "1953.00")
    private BigDecimal amount;

    /**
     * TRADE (brokerage, platform, exchange) or WITHDRAWAL.
     */
    @NotNull(message = "Kind is required")
    private FeeCalculationKind kind;
}
