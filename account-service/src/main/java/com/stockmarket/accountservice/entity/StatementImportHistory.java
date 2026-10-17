package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.ImportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * One attempt to import a statement: the first import and every reprocess.
 *
 * {@code reported*} fields hold the broker's own P&amp;L summary from the file (when it
 * has one), so the import can be reconciled against what was recorded.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "statement_import_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_import_history_attempt",
                columnNames = {"statement_id", "attempt_number"})
)
public class StatementImportHistory extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private AccountStatement statement;

    @Min(1)
    @Column(nullable = false)
    private int attemptNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ImportStatus status;

    /**
     * Simple class name of the parser used, for example {@code ZerodhaParser}.
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String parserName;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private int rowsRead;

    @Column(nullable = false)
    private int rowsSkipped;

    @Column(nullable = false)
    private int tradesImported;

    /**
     * Trades skipped because their broker trade id was already imported.
     */
    @Column(nullable = false)
    private int duplicateTrades;

    @Column(nullable = false)
    private int chargesImported;

    @Column(nullable = false)
    private int taxesImported;

    @Column(precision = 19, scale = 2)
    private BigDecimal reportedRealizedPnl;

    @Column(precision = 19, scale = 2)
    private BigDecimal reportedUnrealizedPnl;

    @Column(precision = 19, scale = 2)
    private BigDecimal reportedCharges;

    @Column(precision = 19, scale = 2)
    private BigDecimal reportedTaxes;

    @Column(precision = 19, scale = 2)
    private BigDecimal reportedNetPnl;

    /**
     * Newline-separated warnings: skipped rows, unmapped labels, duplicates.
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String warnings;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
