package com.stockmarket.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The settings a user chose (theme, language, notification channels, export format), so they follow the user across
 * devices. One row per user, created on the first save; a user without a row has the defaults below.
 */
@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false, length = 10)
    private String theme = "SYSTEM";

    @Column(nullable = false, length = 10)
    private String language = "en";

    @Column(name = "notify_in_app", nullable = false)
    private boolean notifyInApp = true;

    // Opt-in channels: nothing is sent to a channel the user has not switched on.
    @Column(name = "notify_push", nullable = false)
    private boolean notifyPush = false;

    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail = false;

    @Column(name = "export_format", nullable = false, length = 10)
    private String exportFormat = "CSV";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserPreferences(String userId) {
        this.userId = userId;
    }
}
