package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.ChargeType;
import com.stockmarket.accountservice.enums.RecordSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A charge recorded from a statement or manual trade.
 */
@Schema(description = "A charge recorded from a statement or manual trade")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountChargeResponse {

    /**
     * Id.
     */
    private String id;

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Charge type.
     */
    private ChargeType chargeType;

    /**
     * Amount.
     */
    private BigDecimal amount;

    /**
     * Date.
     */
    private LocalDate chargeDate;

    /**
     * MANUAL or IMPORT.
     */
    private RecordSource source;

    /**
     * Trade, if per-trade.
     */
    private String tradeId;

    /**
     * Statement, if imported.
     */
    private String statementId;

    /**
     * Label or description.
     */
    private String description;
}
