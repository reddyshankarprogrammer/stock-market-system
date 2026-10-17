package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tax preview from the tax engine.
 */
@Schema(description = "Tax preview from the tax engine")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxBreakdownResponse {

    /**
     * One line per tax rule that applies.
     */
    private List<ChargeLineResponse> taxes;

    /**
     * Sum of all lines.
     */
    private BigDecimal totalTax;
}
