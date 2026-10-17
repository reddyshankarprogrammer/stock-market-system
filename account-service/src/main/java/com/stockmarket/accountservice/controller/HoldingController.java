package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.response.HoldingResponse;
import com.stockmarket.accountservice.service.HoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Positions held in an account.
 *
 * Base URL: /api/v1/holdings
 */
@Tag(name = "Holdings", description = "Shares held, valued at live prices")
@RestController
@RequestMapping("/api/v1/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;

    @Operation(summary = "List an account's open positions",
            description = "Live price fields are null when market-data-service is unavailable.")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<HoldingResponse>> getHoldings(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(holdingService.getHoldings(accountId, userId));
    }
}
