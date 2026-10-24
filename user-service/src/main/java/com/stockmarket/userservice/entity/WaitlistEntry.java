package com.stockmarket.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Someone who asked, on the public landing page, to be told when Quantiva opens. Only the email (lower case, unique)
 * and the time they agreed to be contacted are kept. It is not an account: no password, no access, no email is sent
 * from here.
 */
@Entity
@Table(name = "waitlist_entries", uniqueConstraints = @UniqueConstraint(name = "uk_waitlist_email", columnNames = "email"))
@Data
@NoArgsConstructor
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 254)
    private String email;

    /** When the visitor ticked the consent box (server clock). */
    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    /** Where the form was shown, for example "landing". */
    @Column(length = 40)
    private String source;
}
