package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A total for one calendar month.
 */
@Schema(description = "A total for one calendar month")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthAmountResponse {

    /**
     * Month 1-12.
     */
    private int month;

    /**
     * Total.
     */
    private BigDecimal amount;
}
