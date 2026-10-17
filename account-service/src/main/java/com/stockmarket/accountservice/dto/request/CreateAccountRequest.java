package com.stockmarket.accountservice.dto.request;

import com.stockmarket.accountservice.enums.AccountType;
import com.stockmarket.accountservice.enums.BrokerName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Opens a new investment account for the authenticated user.
 */
@Schema(description = "Opens a new investment account for the authenticated user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    /**
     * Display name chosen by the user.
     */
    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name must be at most 100 characters")
    @Schema(example = "Long-term growth")
    private String accountName;

    /**
     * INDIVIDUAL, JOINT or CORPORATE.
     */
    @NotNull(message = "Account type is required")
    private AccountType accountType;

    /**
     * ISO 4217 currency of the balance; defaults to USD (stock prices are in USD).
     */
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO code")
    @Schema(example = "USD", defaultValue = "USD")
    private String currency;

    /**
     * Where the account is held; defaults to PLATFORM. Brokers other than PLATFORM/PAPER are tracking accounts.
     */
    @Schema(example = "ZERODHA", defaultValue = "PLATFORM")
    private BrokerName brokerName;

    /**
     * Free-text notes.
     */
    @Size(max = 500, message = "Remarks must be at most 500 characters")
    @Schema(example = "Zerodha Equity - long term")
    private String remarks;
}
