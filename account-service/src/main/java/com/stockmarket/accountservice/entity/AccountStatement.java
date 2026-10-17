package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.BrokerName;
import com.stockmarket.accountservice.enums.StatementFormat;
import com.stockmarket.accountservice.enums.StatementStatus;
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
import jakarta.validation.constraints.NotBlank;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A broker statement file uploaded to a tracking account.
 *
 * The file is stored on disk at {@code storagePath} (relative to
 * {@code account.statements.storage-dir}); this row holds its metadata.
 * The SHA-256 checksum is unique per account, so the same file cannot be imported twice.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_statements",
        indexes = {
                @Index(name = "idx_statements_account_uploaded", columnList = "account_id, uploaded_at"),
                @Index(name = "idx_statements_user", columnList = "user_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_statements_account_checksum",
                columnNames = {"account_id", "checksum_sha256"})
)
public class AccountStatement extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String userId;

    /**
     * Broker of the account at upload time; selects the parser.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private BrokerName brokerName;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String originalFileName;

    /**
     * Path of the stored file relative to the storage directory, for example
     * {@code <accountId>/<statementId>.xlsx}. Never derived from the uploaded name.
     */
    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String storagePath;

    @Size(max = 100)
    @Column(length = 100)
    private String contentType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private StatementFormat fileFormat;

    @Positive
    @Column(nullable = false)
    private long fileSize;

    @NotBlank
    @Size(max = 64)
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private StatementStatus status;

    /**
     * First and last trade date found in the file, once processed.
     */
    private LocalDate periodFrom;

    private LocalDate periodTo;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;
}
