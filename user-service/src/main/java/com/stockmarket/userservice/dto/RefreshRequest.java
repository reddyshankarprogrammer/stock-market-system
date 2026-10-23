package com.stockmarket.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @ToString.Exclude
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
