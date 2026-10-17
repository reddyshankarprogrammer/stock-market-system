package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.LedgerEntryType;
import com.stockmarket.accountservice.enums.ReferenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
 * One immutable entry in an account's cash ledger.
 *
 * Every change to {@link InvestmentAccount#getBalance()} has exactly one ledger entry.
 * {@code balanceAfter} is the running balance right after this entry, so the ledger
 * alone can reproduce the balance at any point in time. Entries are ordered by
 * {@code entryNumber}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_ledger",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ledger_account_entry",
                columnNames = {"account_id", "entry_number"}),
        indexes = {
                @Index(name = "idx_ledger_account_date", columnList = "account_id, transaction_date"),
                @Index(name = "idx_ledger_reference", columnList = "reference_type, reference_id")
        }
)
public class AccountLedger extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    /**
     * Position of this entry in the account's ledger: 1, 2, 3, ...
     * Assigned while the account row is locked, so it is gap-free and gives a total
     * order even when several entries share the same timestamp.
     */
    @Column(nullable = false)
    private long entryNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private LedgerEntryType entryType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LedgerCategory category;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ReferenceType referenceType;

    /**
     * Id of the trade, fund transaction, dividend or interest record that caused this entry.
     */
    @NotNull
    @Column(nullable = false, length = 36)
    private String referenceId;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime transactionDate;
}
