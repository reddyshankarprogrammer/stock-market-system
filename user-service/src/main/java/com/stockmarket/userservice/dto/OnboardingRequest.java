package com.stockmarket.userservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One step or several at a time: every field is optional and only the fields that are sent are changed, so the web
 * app can save after each step and a half-finished onboarding can be resumed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingRequest {

    @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED|PROFESSIONAL)$", message = "Trading experience should be BEGINNER, INTERMEDIATE, ADVANCED or PROFESSIONAL")
    private String tradingExperience;

    /** GROWW, ZERODHA, BINANCE, EXNESS, OTHER, or NONE (no broker yet; cannot be combined with the others). */
    @Size(max = 6, message = "At most 6 brokers")
    private List<@Pattern(regexp = "^(GROWW|ZERODHA|BINANCE|EXNESS|OTHER|NONE)$", message = "Unknown broker") String> brokerPreferences;

    /** The plan the user is interested in. Recorded only: billing and entitlements do not exist yet. */
    @Pattern(regexp = "^(FREE|PRO|AI_PRO|QUANT_PRO|ENTERPRISE)$", message = "Plan should be FREE, PRO, AI_PRO, QUANT_PRO or ENTERPRISE")
    private String plan;
}
