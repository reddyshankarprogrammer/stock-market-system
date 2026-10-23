package com.stockmarket.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @ToString.Exclude
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    // Same rule as registration and reset. At most 72 characters: BCrypt only uses the first 72 bytes.
    @ToString.Exclude
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 72, message = "Password should be 6 to 72 characters")
    private String newPassword;
}
