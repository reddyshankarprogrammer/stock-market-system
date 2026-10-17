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
 * Pays cash into an account.
 */
@Schema(description = "Pays cash into an account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    /**
     * Amount to deposit.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(example = "5000.00")
    private BigDecimal amount;

    /**
     * Optional external reference, for example a bank transfer id.
     */
    @Size(max = 100, message = "Reference must be at most 100 characters")
    @Schema(example = "BANK-TRF-0001")
    private String reference;
}
