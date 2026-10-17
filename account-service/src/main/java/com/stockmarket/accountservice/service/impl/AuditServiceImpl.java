package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.response.AuditLogResponse;
import com.stockmarket.accountservice.entity.AuditLog;
import com.stockmarket.accountservice.enums.AuditAction;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AuditLogRepository;
import com.stockmarket.accountservice.security.RequestUser;
import com.stockmarket.accountservice.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    /**
     * Upper bound per stored payload, so one huge response cannot bloat the audit table.
     */
    private static final int MAX_JSON_LENGTH = 100_000;

    private final AuditLogRepository auditLogRepository;
    private final AccountMapper mapper;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void logAccountAction(String accountId, AuditAction action, String details) {
        logAccountAction(accountId, action, details, null, null);
    }

    /**
     * MANDATORY: the entry must commit or roll back together with the change it records.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void logAccountAction(String accountId, AuditAction action, String details, Object request, Object response) {

        String performedBy = RequestUser.userId();

        auditLogRepository.save(AuditLog.builder()
                .entityType(ACCOUNT_ENTITY)
                .entityId(accountId)
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .requestJson(toJson(request))
                .responseJson(toJson(response))
                .build());

        log.info("AUDIT account={} action={} by={} {}", accountId, action, performedBy, details);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAccountAuditTrail(String accountId) {
        return mapper.toAuditLogResponses(
                auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(ACCOUNT_ENTITY, accountId));
    }

    /**
     * Serializes a payload; an audit entry is still written if serialization fails.
     */
    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            String json = jsonMapper.writeValueAsString(payload);
            return json.length() > MAX_JSON_LENGTH ? json.substring(0, MAX_JSON_LENGTH) : json;
        } catch (JacksonException e) {
            log.warn("Could not serialize audit payload {}: {}", payload.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
}
