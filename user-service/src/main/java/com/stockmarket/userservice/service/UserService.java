package com.stockmarket.userservice.service;

import com.stockmarket.userservice.dto.*;

import java.math.BigDecimal;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /** Exchange a valid refresh token for a new access token. The refresh token is returned unchanged (not rotated). */
    AuthResponse refresh(RefreshRequest request);

    /**
     * Change the caller's own password. Ends every session that started before the change (like a reset) and returns
     * fresh tokens, so the session that made the change carries on.
     */
    AuthResponse changePassword(String userId, ChangePasswordRequest request);

    UserResponse getUserById(String userId);

    UserResponse addFunds(String userId, BigDecimal amount);

    UserResponse deductFunds(String userId, BigDecimal amount);

    UserResponse creditFunds(String userId, BigDecimal amount);
}