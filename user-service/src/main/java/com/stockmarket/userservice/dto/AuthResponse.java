package com.stockmarket.userservice.dto;

import lombok.*;


import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private BigDecimal walletBalance;

    private String accessToken;
    private String refreshToken;

    private String tokenType = "Bearer";
}