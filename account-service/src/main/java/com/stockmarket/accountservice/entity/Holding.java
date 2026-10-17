package com.stockmarket.accountservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shares of one symbol held in an investment account.
 *
 * The row is kept when the quantity drops to zero, so {@code realizedProfit}
 * (cumulative over all SELL trades of the symbol) is not lost.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_holdings",
        indexes = @Index(name = "idx_holdings_user", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_holdings_account_symbol",
                columnNames = {"account_id", "symbol"})
)
public class Holding extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String userId;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String symbol;

    /**
     * Shares, coins or lots held; fractional for crypto and forex.
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    /**
     * Weighted average purchase price per unit (excluding fees and taxes).
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal averagePrice;

    /**
     * Cost basis of the units still held (the invested amount): averagePrice x quantity.
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost;

    /**
     * Cumulative realized profit (negative for a loss) from SELL trades of this symbol.
     */
    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal realizedProfit;

    /**
     * Last known price; refreshed from market-data-service when available.
     */
    @Column(precision = 19, scale = 8)
    private BigDecimal currentPrice;

    /**
     * currentPrice x quantity at {@code priceUpdatedAt}.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal marketValue;

    /**
     * marketValue - totalCost at {@code priceUpdatedAt}.
     */
    @Column(name = "unrealized_pnl", precision = 19, scale = 2)
    private BigDecimal unrealizedPnl;

    private LocalDateTime priceUpdatedAt;
}
