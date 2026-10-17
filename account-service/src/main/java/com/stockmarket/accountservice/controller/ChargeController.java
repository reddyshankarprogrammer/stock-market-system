package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.response.AccountChargeResponse;
import com.stockmarket.accountservice.dto.response.ChargeSummaryResponse;
import com.stockmarket.accountservice.service.ChargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

/**
 * Charges recorded from statements and manual trades.
 *
 * Base URL: /api/v1/charges
 */
@Tag(name = "Charges", description = "Imported and manual charges, and yearly charge summaries")
@Validated
@RestController
@RequestMapping("/api/v1/charges")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;

    @Operation(summary = "Charges of an account", description = "Imported and manual charges, newest first.")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<AccountChargeResponse>> getCharges(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(chargeService.getCharges(accountId, userId));
    }

    @Operation(summary = "Yearly charge summary",
            description = "Totals per charge type for a calendar year (default: current year), including the "
                    + "platform fee engine's fees.")
    @GetMapping("/account/{accountId}/summary")
    public ResponseEntity<ChargeSummaryResponse> getChargeSummary(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year) {

        return ResponseEntity.ok(chargeService.getChargeSummary(accountId, userId,
                year == null ? Year.now().getValue() : year));
    }
}
