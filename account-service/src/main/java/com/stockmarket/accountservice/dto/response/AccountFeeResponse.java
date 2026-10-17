package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.FeeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A fee charged to an account.
 */
@Schema(description = "A fee charged to an account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountFeeResponse {

    /**
     * Id.
     */
    private String id;

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Fee type.
     */
    private FeeType feeType;

    /**
     * Fee schedule code used.
     */
    private String feeCode;

    /**
     * Trade, if charged on a trade.
     */
    private String tradeId;

    /**
     * Withdrawal, if charged on a withdrawal.
     */
    private String fundTransactionId;

    /**
     * Amount the fee was calculated on.
     */
    private BigDecimal baseAmount;

    /**
     * Fee charged.
     */
    private BigDecimal feeAmount;

    /**
     * When it was charged.
     */
    private LocalDateTime chargedAt;
}
