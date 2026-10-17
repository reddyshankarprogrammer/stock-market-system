package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.TaxType;
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
 * A tax charged to an account: by the tax engine (PLATFORM), entered with a manual trade
 * (MANUAL) or imported from a broker statement (IMPORT).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_taxes",
        indexes = {
                @Index(name = "idx_account_taxes_account_date", columnList = "account_id, charged_at"),
                @Index(name = "idx_account_taxes_type", columnList = "tax_type"),
                @Index(name = "idx_account_taxes_statement", columnList = "statement_id")
        }
)
public class AccountTax extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    /**
     * Rule that produced the tax; null for imported and manual taxes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_master_id")
    private TaxMaster taxMaster;

    /**
     * Set when the tax was charged on a trade.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id")
    private Trade trade;

    /**
     * Set when the tax was withheld from a dividend.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dividend_id")
    private Dividend dividend;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TaxType taxType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RecordSource source = RecordSource.PLATFORM;

    /**
     * Statement the tax was imported from.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id")
    private AccountStatement statement;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    /**
     * Amount the tax was calculated on (trade value, realized profit or gross dividend).
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxableAmount;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime chargedAt;
}
