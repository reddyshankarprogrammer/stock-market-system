package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.TaxType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A tax charged to an account.
 */
@Schema(description = "A tax charged to an account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTaxResponse {

    /**
     * Id.
     */
    private String id;

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Tax type.
     */
    private TaxType taxType;

    /**
     * Tax rule code used.
     */
    private String taxCode;

    /**
     * Trade, if charged on a trade.
     */
    private String tradeId;

    /**
     * Dividend, if withheld from a dividend.
     */
    private String dividendId;

    /**
     * PLATFORM (tax engine), MANUAL or IMPORT.
     */
    private RecordSource source;

    /**
     * Statement it was imported from.
     */
    private String statementId;

    /**
     * Label or description.
     */
    private String description;

    /**
     * Amount the tax was calculated on.
     */
    private BigDecimal taxableAmount;

    /**
     * Tax charged.
     */
    private BigDecimal taxAmount;

    /**
     * When it was charged.
     */
    private LocalDateTime chargedAt;
}
