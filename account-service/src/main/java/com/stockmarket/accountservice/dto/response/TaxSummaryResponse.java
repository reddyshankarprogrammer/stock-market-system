package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Taxes of an account for a year.
 */
@Schema(description = "Taxes of an account for a year")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSummaryResponse {

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Calendar year.
     */
    private int year;

    /**
     * Total per tax type, largest first.
     */
    private List<TypeAmountResponse> byType;

    /**
     * Total per month that had taxes.
     */
    private List<MonthAmountResponse> byMonth;

    /**
     * Sum of all types.
     */
    private BigDecimal totalTaxes;
}
