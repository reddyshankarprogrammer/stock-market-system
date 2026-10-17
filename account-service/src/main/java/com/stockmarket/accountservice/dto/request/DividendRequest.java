package com.stockmarket.accountservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Posts a cash dividend for a symbol held in the account (internal, service-to-service).
 */
@Schema(description = "Posts a cash dividend for a symbol held in the account (internal, service-to-service)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DividendRequest {

    /**
     * Symbol paying the dividend.
     */
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "[A-Za-z.]{1,20}", message = "Symbol must be 1-20 letters")
    @Schema(example = "AAPL")
    private String symbol;

    /**
     * Cash paid per share.
     */
    @NotNull(message = "Dividend per share is required")
    @DecimalMin(value = "0.0001", message = "Dividend per share must be positive")
    @Digits(integer = 15, fraction = 4, message = "Dividend per share must have at most 4 decimal places")
    @Schema(example = "0.25")
    private BigDecimal dividendPerShare;

    /**
     * Date the dividend is paid.
     */
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;
}
