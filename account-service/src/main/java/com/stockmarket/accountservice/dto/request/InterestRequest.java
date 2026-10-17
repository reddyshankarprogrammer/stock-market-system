package com.stockmarket.accountservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Posts interest on the cash balance for a period (internal, service-to-service).
 */
@Schema(description = "Posts interest on the cash balance for a period (internal, service-to-service)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRequest {

    /**
     * Annual rate in percent.
     */
    @NotNull(message = "Annual rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Annual rate must be positive")
    @DecimalMax(value = "100.0", message = "Annual rate must be at most 100")
    @Schema(example = "4.5")
    private BigDecimal annualRate;

    /**
     * First day of the period (inclusive).
     */
    @NotNull(message = "Period start is required")
    private LocalDate periodStart;

    /**
     * Last day of the period (inclusive).
     */
    @NotNull(message = "Period end is required")
    private LocalDate periodEnd;
}
