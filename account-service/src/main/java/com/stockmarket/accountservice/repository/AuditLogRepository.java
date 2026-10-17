package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);

    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy);
}
