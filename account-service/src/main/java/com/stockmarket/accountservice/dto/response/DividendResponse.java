package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A dividend paid into an account.
 */
@Schema(description = "A dividend paid into an account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DividendResponse {

    /**
     * Dividend id.
     */
    private String id;

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Symbol.
     */
    private String symbol;

    /**
     * Shares held when paid.
     */
    private BigDecimal quantity;

    /**
     * Cash per share.
     */
    private BigDecimal dividendPerShare;

    /**
     * Before tax.
     */
    private BigDecimal grossAmount;

    /**
     * Withholding tax.
     */
    private BigDecimal taxAmount;

    /**
     * Credited to the account.
     */
    private BigDecimal netAmount;

    /**
     * Payment date.
     */
    private LocalDate paymentDate;
}
