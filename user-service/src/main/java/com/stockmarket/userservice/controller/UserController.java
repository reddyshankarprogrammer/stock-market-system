package com.stockmarket.userservice.controller;

import com.stockmarket.userservice.dto.AuthResponse;
import com.stockmarket.userservice.dto.LoginRequest;
import com.stockmarket.userservice.dto.RefreshRequest;
import com.stockmarket.userservice.dto.RegisterRequest;
import com.stockmarket.userservice.dto.UserResponse;
import com.stockmarket.userservice.exception.UserAccessDeniedException;
import com.stockmarket.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(userService.login(request));
    }

    /**
     * Public (the gateway cannot require an access token here: the old one has expired). The refresh token travels
     * in the body, never in the Authorization header, because the gateway rejects refresh tokens on protected paths.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        return ResponseEntity.ok(userService.refresh(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(
                userService.getUserById(userId));
    }

    /**
     * Own profile, or any profile for an ADMIN (X-User-Role is set by the api-gateway from the JWT).
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String authenticatedUserId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        if (!"ADMIN".equals(role)) {
            requireOwner(userId, authenticatedUserId);
        }

        return ResponseEntity.ok(
                userService.getUserById(userId));
    }

    @PostMapping("/{userId}/funds/add")
    public ResponseEntity<UserResponse> addFunds(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String authenticatedUserId,
            @RequestParam BigDecimal amount) {

        requireOwner(userId, authenticatedUserId);

        return ResponseEntity.ok(
                userService.addFunds(userId, amount));
    }

    // deduct/credit are service-to-service calls from order-service (blocked at the api-gateway),
    // so they carry no X-User-Id and have no ownership check.
    @PostMapping("/{userId}/funds/deduct")
    public ResponseEntity<UserResponse> deductFunds(
            @PathVariable String userId,
            @RequestParam BigDecimal amount) {

        return ResponseEntity.ok(
                userService.deductFunds(userId, amount));
    }

    @PostMapping("/{userId}/funds/credit")
    public ResponseEntity<UserResponse> creditFunds(
            @PathVariable String userId,
            @RequestParam BigDecimal amount) {

        return ResponseEntity.ok(
                userService.creditFunds(userId, amount));
    }

    /**
     * X-User-Id is set by the api-gateway from the validated JWT.
     */
    private void requireOwner(String userId, String authenticatedUserId) {

        if (!userId.equals(authenticatedUserId)) {
            log.warn("User {} tried to access user {}", authenticatedUserId, userId);
            throw new UserAccessDeniedException(userId);
        }
    }
}