package com.stockmarket.accountservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Interest posted to an account.
 */
@Schema(description = "Interest posted to an account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestResponse {

    /**
     * Id.
     */
    private String id;

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Balance the interest was calculated on.
     */
    private BigDecimal principalAmount;

    /**
     * Annual rate in percent.
     */
    private BigDecimal annualRate;

    /**
     * Days in the period.
     */
    private int days;

    /**
     * Interest credited.
     */
    private BigDecimal interestAmount;

    /**
     * Period start.
     */
    private LocalDate periodStart;

    /**
     * Period end.
     */
    private LocalDate periodEnd;

    /**
     * When it was posted.
     */
    private LocalDateTime postedAt;
}
