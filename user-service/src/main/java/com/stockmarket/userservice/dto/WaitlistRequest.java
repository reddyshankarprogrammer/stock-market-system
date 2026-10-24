package com.stockmarket.userservice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 254, message = "Email should be at most 254 characters")
    private String email;

    /** Must be true: the visitor agrees to be contacted about the launch. */
    @AssertTrue(message = "Please agree to be contacted about the launch")
    private boolean consent;

    @Pattern(regexp = "^[a-z0-9-]{1,40}$", message = "Source should be lower-case letters, digits or dashes")
    private String source;
}
