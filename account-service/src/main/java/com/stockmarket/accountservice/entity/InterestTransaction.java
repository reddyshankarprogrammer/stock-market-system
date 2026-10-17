package com.stockmarket.accountservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Interest paid on an account's cash balance for a period.
 *
 * Simple interest: {@code principalAmount x annualRate / 100 x days / 365}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_interest_transactions",
        indexes = @Index(name = "idx_interest_account_posted", columnList = "account_id, posted_at")
)
public class InterestTransaction extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    /**
     * Cash balance the interest was calculated on.
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    /**
     * Annual rate in percent (4.5 means 4.5%).
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal annualRate;

    @Min(1)
    @Column(nullable = false)
    private int days;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @NotNull
    @Column(nullable = false)
    private LocalDate periodStart;

    @NotNull
    @Column(nullable = false)
    private LocalDate periodEnd;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime postedAt;
}
