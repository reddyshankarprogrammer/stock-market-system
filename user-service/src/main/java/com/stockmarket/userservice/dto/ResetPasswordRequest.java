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
public class ResetPasswordRequest {

    @ToString.Exclude
    @NotBlank(message = "Reset token is required")
    private String token;

    // Same minimum as registration. At most 72 characters: BCrypt only uses the first 72 bytes of a password.
    @ToString.Exclude
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 72, message = "Password should be 6 to 72 characters")
    private String newPassword;
}
