package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.CalculationType;
import com.stockmarket.accountservice.enums.TaxType;
import com.stockmarket.accountservice.enums.TradeSide;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A tax rule.
 */
@Schema(description = "A tax rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxMasterResponse {

    /**
     * Id.
     */
    private String id;

    /**
     * Unique code.
     */
    private String code;

    /**
     * Tax type.
     */
    private TaxType taxType;

    /**
     * FIXED or PERCENTAGE.
     */
    private CalculationType calculationType;

    /**
     * Amount or percentage.
     */
    private BigDecimal rate;

    /**
     * BUY or SELL for trade taxes; null for both.
     */
    private TradeSide appliesToSide;

    /**
     * Description.
     */
    private String description;
}
