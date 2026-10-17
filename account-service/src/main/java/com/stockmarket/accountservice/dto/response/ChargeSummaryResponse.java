package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Charges of an account for a year: imported/manual charges and platform fee-engine fees,
 * mapped onto charge types.
 */
@Schema(description = "Charges of an account for a year: imported/manual charges and platform fee-engine fees,")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeSummaryResponse {

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Calendar year.
     */
    private int year;

    /**
     * Total per charge type, largest first.
     */
    private List<TypeAmountResponse> byType;

    /**
     * Sum of all types.
     */
    private BigDecimal totalCharges;
}
