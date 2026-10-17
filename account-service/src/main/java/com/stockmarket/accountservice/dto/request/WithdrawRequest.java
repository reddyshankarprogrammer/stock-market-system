package com.stockmarket.accountservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Pays cash out of an account. The withdrawal fee is charged on top of the amount.
 */
@Schema(description = "Pays cash out of an account. The withdrawal fee is charged on top of the amount")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequest {

    /**
     * Amount to withdraw (excluding the withdrawal fee).
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(example = "250.00")
    private BigDecimal amount;

    /**
     * Optional external reference.
     */
    @Size(max = 100, message = "Reference must be at most 100 characters")
    private String reference;
}
