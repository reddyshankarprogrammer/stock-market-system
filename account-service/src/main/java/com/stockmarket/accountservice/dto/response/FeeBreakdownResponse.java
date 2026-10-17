package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fee preview from the fee engine.
 */
@Schema(description = "Fee preview from the fee engine")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeBreakdownResponse {

    /**
     * Amount the fees were calculated on.
     */
    private BigDecimal baseAmount;

    /**
     * One line per fee type.
     */
    private List<ChargeLineResponse> fees;

    /**
     * Sum of all lines.
     */
    private BigDecimal totalFees;
}
