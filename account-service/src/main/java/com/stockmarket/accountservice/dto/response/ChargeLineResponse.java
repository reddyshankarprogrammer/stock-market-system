package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One line of a fee or tax breakdown.
 */
@Schema(description = "One line of a fee or tax breakdown")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeLineResponse {

    /**
     * Fee or tax rule code.
     */
    private String code;

    /**
     * Fee type or tax type.
     */
    private String type;

    /**
     * Amount the charge was calculated on.
     */
    private BigDecimal baseAmount;

    /**
     * Charge.
     */
    private BigDecimal amount;
}
