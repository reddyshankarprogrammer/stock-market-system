package com.stockmarket.accountservice.config;

import com.stockmarket.accountservice.security.RequestUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables Spring Data JPA auditing for {@code BaseEntity}
 * (createdAt, updatedAt, createdBy, updatedBy).
 *
 * The auditor is the {@code X-User-Id} header of the current request, which the
 * api-gateway sets from the JWT. Internal calls without the header, and work outside
 * a request, are recorded as {@value RequestUser#SYSTEM}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(RequestUser.userId());
    }

    /**
     * The calling user's id, or {@value RequestUser#SYSTEM}.
     */
    public static String currentUserId() {
        return RequestUser.userId();
    }
}
