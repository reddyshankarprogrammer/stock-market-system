package com.stockmarket.accountservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A cash dividend paid on shares held in an account.
 *
 * {@code netAmount} = {@code grossAmount} - {@code taxAmount} (withholding tax).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_dividends",
        indexes = {
                @Index(name = "idx_dividends_account_date", columnList = "account_id, payment_date"),
                @Index(name = "idx_dividends_symbol", columnList = "symbol")
        }
)
public class Dividend extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String symbol;

    /**
     * Shares held when the dividend was paid.
     */
    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal dividendPerShare;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @NotNull
    @Column(nullable = false)
    private LocalDate paymentDate;
}
