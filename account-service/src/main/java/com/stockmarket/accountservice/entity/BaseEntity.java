package com.stockmarket.accountservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Common columns for every account-service table.
 *
 * <ul>
 *   <li>UUID primary key stored as a 36-character string, like the other services.</li>
 *   <li>{@code @Version} for optimistic locking: a concurrent update of the same row
 *       fails with {@code ObjectOptimisticLockingFailureException} (HTTP 409).</li>
 *   <li>Spring Data auditing: timestamps and the user id from {@code X-User-Id}
 *       (see {@code AuditConfig}).</li>
 * </ul>
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(length = 64, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 64)
    private String updatedBy;
}
