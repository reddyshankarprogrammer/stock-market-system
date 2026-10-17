package com.stockmarket.userservice.service;

import com.stockmarket.userservice.dto.*;

import java.math.BigDecimal;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getUserById(String userId);

    UserResponse addFunds(String userId, BigDecimal amount);

    UserResponse deductFunds(String userId, BigDecimal amount);

    UserResponse creditFunds(String userId, BigDecimal amount);
}