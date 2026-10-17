package com.stockmarket.accountservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The fields of user-service's UserResponse that account-service uses.
 *
 * @param status ACTIVE, INACTIVE, SUSPENDED, ... (user-service's UserStatus)
 * @param role   USER or ADMIN
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserDto(String id, String email, String firstName, String lastName, String status, String role) {

    public static final String ACTIVE = "ACTIVE";

    public boolean isActive() {
        return ACTIVE.equals(status);
    }
}
