package com.stockmarket.accountservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Adjusts the cash balance to reconcile with a broker statement. Positive adds cash, negative removes it.
 */
@Schema(description = "Adjusts the cash balance to reconcile with a broker statement. Positive adds cash, negative removes it")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentRequest {

    /**
     * Signed amount; must not be zero.
     */
    @NotNull(message = "Amount is required")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(example = "-125.40")
    private BigDecimal amount;

    /**
     * Why the adjustment is needed.
     */
    @NotBlank(message = "Description is required")
    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;

    /**
     * Statement this adjustment reconciles, if any.
     */
    @Size(max = 36, message = "Statement id must be at most 36 characters")
    private String statementId;
}
