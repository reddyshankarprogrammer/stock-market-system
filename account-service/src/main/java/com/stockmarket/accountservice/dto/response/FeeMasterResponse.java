package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.CalculationType;
import com.stockmarket.accountservice.enums.FeeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * An entry of the fee schedule.
 */
@Schema(description = "An entry of the fee schedule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeMasterResponse {

    /**
     * Id.
     */
    private String id;

    /**
     * Unique code.
     */
    private String code;

    /**
     * Fee type.
     */
    private FeeType feeType;

    /**
     * FIXED or PERCENTAGE.
     */
    private CalculationType calculationType;

    /**
     * Amount or percentage.
     */
    private BigDecimal value;

    /**
     * Minimum fee, if any.
     */
    private BigDecimal minAmount;

    /**
     * Maximum fee, if any.
     */
    private BigDecimal maxAmount;

    /**
     * Description.
     */
    private String description;
}
