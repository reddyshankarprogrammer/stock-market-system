package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A total for one type.
 */
@Schema(description = "A total for one type")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeAmountResponse {

    /**
     * Charge or tax type.
     */
    private String type;

    /**
     * Total.
     */
    private BigDecimal amount;
}
