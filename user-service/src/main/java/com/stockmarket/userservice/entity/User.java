package com.stockmarket.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Existing rows get USER through the column default when Hibernate adds the column.
     * Promote a user with: UPDATE users SET role = 'ADMIN' WHERE email = '...';
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) not null default 'USER'")
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Profile fields (all nullable: existing users have none, so Hibernate can add the columns without defaults).
    @Column(length = 8)
    private String mobileCountryCode;

    @Column(length = 20)
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 2)
    private String country;

    /** Version of the Terms and Risk Disclaimer accepted at registration, and when (server time). */
    @Column(length = 50)
    private String termsVersion;

    private LocalDateTime termsAcceptedAt;

    /** Set by a password reset. Refresh tokens issued before this moment are refused. Null if never reset. */
    private LocalDateTime passwordChangedAt;

    // Onboarding answers (all nullable: existing users have not been through it). Plan is an interest only, not an entitlement.
    @Column(length = 20)
    private String tradingExperience;

    /** Comma-separated broker codes, for example "EXNESS,GROWW". */
    @Column(length = 100)
    private String brokerPreferences;

    @Column(length = 20)
    private String selectedPlan;

    /** Set when onboarding is completed; null while it is not. */
    private LocalDateTime onboardingCompletedAt;
}


