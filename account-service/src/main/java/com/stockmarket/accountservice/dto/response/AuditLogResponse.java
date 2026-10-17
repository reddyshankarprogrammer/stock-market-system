package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.AuditAction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One audit trail entry.
 */
@Schema(description = "One audit trail entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    /**
     * Id.
     */
    private String id;

    /**
     * Audited record type.
     */
    private String entityType;

    /**
     * Audited record id.
     */
    private String entityId;

    /**
     * Action.
     */
    private AuditAction action;

    /**
     * User id or 'system'.
     */
    private String performedBy;

    /**
     * Details.
     */
    private String details;

    /**
     * Request payload (JSON), when there was one.
     */
    private String requestJson;

    /**
     * Result (JSON).
     */
    private String responseJson;

    /**
     * When it happened.
     */
    private LocalDateTime createdAt;
}
