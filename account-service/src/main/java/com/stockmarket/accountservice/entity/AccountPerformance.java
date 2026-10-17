package com.stockmarket.accountservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Latest performance snapshot of an account (one row per account, recalculated on request).
 *
 * <pre>
 * grossProfit = realizedProfit + unrealizedProfit + fees already deducted from realized profit
 * netProfit   = grossProfit - totalCharges - totalTaxes
 * </pre>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_performance",
        indexes = @Index(name = "idx_performance_user", columnList = "user_id")
)
public class AccountPerformance extends BaseEntity {

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private InvestmentAccount account;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String userId;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossProfit;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal realizedProfit;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unrealizedProfit;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCharges;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalTaxes;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netProfit;

    /**
     * False when some holdings were valued at their last known or cost price.
     */
    @Column(nullable = false)
    private boolean pricesLive;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime calculatedAt;
}
