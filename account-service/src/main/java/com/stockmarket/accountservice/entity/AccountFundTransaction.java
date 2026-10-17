package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.FundTransactionType;
import com.stockmarket.accountservice.enums.TransactionStatus;
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
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
 * A deposit into or withdrawal from an investment account.
 *
 * For a withdrawal, {@code feeAmount} is the withdrawal fee charged on top of
 * {@code amount}; the account is debited {@code amount + feeAmount}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_fund_transactions",
        indexes = {
                @Index(name = "idx_fund_tx_account_date", columnList = "account_id, transaction_date"),
                @Index(name = "idx_fund_tx_type", columnList = "transaction_type")
        }
)
public class AccountFundTransaction extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private FundTransactionType transactionType;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    /**
     * Account balance after this transaction was posted.
     */
    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    /**
     * Optional external reference, for example a bank transfer id.
     */
    @Size(max = 100)
    @Column(length = 100)
    private String reference;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime transactionDate;
}
