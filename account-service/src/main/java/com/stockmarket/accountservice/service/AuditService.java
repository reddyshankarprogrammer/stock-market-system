package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.response.AuditLogResponse;
import com.stockmarket.accountservice.enums.AuditAction;

import java.util.List;

/**
 * Business audit trail. Entries are written in the caller's transaction.
 */
public interface AuditService {

    String ACCOUNT_ENTITY = "InvestmentAccount";

    /**
     * Records an action on an account, performed by the current {@code X-User-Id}
     * (or "system" for internal calls).
     */
    void logAccountAction(String accountId, AuditAction action, String details);

    /**
     * Same, also storing the request and result as JSON (null values are skipped).
     */
    void logAccountAction(String accountId, AuditAction action, String details, Object request, Object response);

    List<AuditLogResponse> getAccountAuditTrail(String accountId);
}
