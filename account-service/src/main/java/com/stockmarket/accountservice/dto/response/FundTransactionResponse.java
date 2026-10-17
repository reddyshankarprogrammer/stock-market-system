package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.FundTransactionType;
import com.stockmarket.accountservice.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A deposit or withdrawal.
 */
@Schema(description = "A deposit or withdrawal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundTransactionResponse {

    /**
     * Transaction id.
     */
    private String id;

    /**
     * Account id.
     */
    private String accountId;

    /**
     * DEPOSIT or WITHDRAWAL.
     */
    private FundTransactionType transactionType;

    /**
     * Amount requested.
     */
    private BigDecimal amount;

    /**
     * Fee charged on top (withdrawals).
     */
    private BigDecimal feeAmount;

    /**
     * Balance after the transaction.
     */
    private BigDecimal balanceAfter;

    /**
     * External reference.
     */
    private String reference;

    /**
     * Outcome.
     */
    private TransactionStatus status;

    /**
     * When it was posted.
     */
    private LocalDateTime transactionDate;
}
