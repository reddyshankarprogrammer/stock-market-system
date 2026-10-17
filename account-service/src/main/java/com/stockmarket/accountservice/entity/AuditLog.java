package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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

/**
 * Business audit trail: who did what to which record.
 *
 * Written in the same transaction as the change it describes, so an entry exists
 * if and only if the change was committed.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_audit_logs",
        indexes = {
                @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_audit_performed_by", columnList = "performed_by"),
                @Index(name = "idx_audit_created_at", columnList = "created_at")
        }
)
public class AuditLog extends BaseEntity {

    /**
     * Simple class name of the audited record, for example {@code InvestmentAccount}.
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String entityType;

    @NotBlank
    @Size(max = 36)
    @Column(nullable = false, length = 36)
    private String entityId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String performedBy;

    /**
     * Free-text details, for example "BUY 5 AAPL @ 195.30".
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * JSON of the request that caused the action, when there was one.
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String requestJson;

    /**
     * JSON of the result of the action.
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String responseJson;
}
