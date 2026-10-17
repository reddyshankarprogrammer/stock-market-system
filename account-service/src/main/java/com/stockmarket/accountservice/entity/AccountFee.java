package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.FeeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A fee actually charged to an account, linked to the trade or withdrawal that caused it.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_fees",
        indexes = @Index(name = "idx_account_fees_account_date", columnList = "account_id, charged_at")
)
public class AccountFee extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_master_id", nullable = false)
    private FeeMaster feeMaster;

    /**
     * Set when the fee was charged on a trade.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id")
    private Trade trade;

    /**
     * Set when the fee was charged on a withdrawal.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_transaction_id")
    private AccountFundTransaction fundTransaction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private FeeType feeType;

    /**
     * Amount the fee was calculated on (trade value or withdrawal amount).
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime chargedAt;
}
