package com.stockmarket.userservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    @ToString.Exclude
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password should be at least 6 characters")
    private String password;
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    private BigDecimal initialBalance = BigDecimal.valueOf(10000);

    // Profile fields collected by the web app. All optional here so existing clients keep working; the web app
    // makes them mandatory. When given they are validated. The mobile number is stored as entered and is not verified.
    @Pattern(regexp = "^\\+\\d{1,4}$", message = "Mobile country code should look like +91")
    private String mobileCountryCode;
    @Pattern(regexp = "^\\d{6,14}$", message = "Mobile number should be 6 to 14 digits")
    private String mobileNumber;
    // A String checked by pattern (not the enum) so a bad value gives the standard validation error, not a JSON parse error with class names.
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "Gender should be MALE or FEMALE")
    private String gender;
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country should be a 2-letter ISO code such as IN")
    private String country;
    /** Version of the Terms and Risk Disclaimer the user accepted. The acceptance time is set by the server. */
    @Size(max = 50, message = "Terms version is too long")
    private String termsVersion;

    /** Country code and number only make sense together. Ignored by JSON; used by bean validation. */
    @JsonIgnore
    @AssertTrue(message = "Mobile country code and mobile number must be given together")
    public boolean isMobilePairComplete() {
        return (mobileCountryCode == null) == (mobileNumber == null);
    }
}
