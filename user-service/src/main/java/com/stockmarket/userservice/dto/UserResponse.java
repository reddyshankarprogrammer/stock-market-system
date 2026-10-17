package com.stockmarket.userservice.dto;

import com.stockmarket.userservice.entity.Role;
import com.stockmarket.userservice.entity.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private BigDecimal walletBalance;
    private UserStatus status;
    private LocalDateTime createdAt;
    private Role role;
}