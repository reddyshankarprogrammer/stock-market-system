package com.stockmarket.accountservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * OpenFeign client for user-service.
 *
 * The caller's {@code X-User-Id} and {@code X-User-Role} headers are forwarded on every
 * call by {@code FeignConfig}, so user-service applies its own ownership rules:
 * {@link #getUser(UUID)} works for the caller's own id, or any id for an ADMIN.
 */
@FeignClient(name = "user-service", url = "${services.user.url}")
public interface UserServiceClient {

    /**
     * {@code GET /api/v1/users/{userId}}.
     */
    @GetMapping("/api/v1/users/{userId}")
    UserDto getUser(@PathVariable("userId") UUID userId);

    /**
     * {@code GET /api/v1/users/me}: the user identified by the forwarded X-User-Id.
     */
    @GetMapping("/api/v1/users/me")
    UserDto getCurrentUser();
}
