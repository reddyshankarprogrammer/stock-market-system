package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.request.TaxRequest;
import com.stockmarket.accountservice.dto.response.AccountTaxResponse;
import com.stockmarket.accountservice.dto.response.TaxBreakdownResponse;
import com.stockmarket.accountservice.dto.response.TaxMasterResponse;
import com.stockmarket.accountservice.dto.response.TaxSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Year;
import com.stockmarket.accountservice.service.TaxCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tax rules, tax previews and taxes charged.
 *
 * Base URL: /api/v1/taxes
 */
@Tag(name = "Taxes", description = "Tax rules, previews, taxes charged and yearly summaries")
@Validated
@RestController
@RequestMapping("/api/v1/taxes")
@RequiredArgsConstructor
public class TaxController {

    private final TaxCalculationService taxCalculationService;

    @Operation(summary = "Active tax rules")
    @GetMapping
    public ResponseEntity<List<TaxMasterResponse>> getTaxRules() {
        return ResponseEntity.ok(taxCalculationService.getTaxRules());
    }

    @Operation(summary = "Preview taxes",
            description = "Trade tax, plus capital gains tax for a SELL with a positive realizedProfit. Nothing is charged.")
    @PostMapping("/calculate")
    public ResponseEntity<TaxBreakdownResponse> previewTax(@Valid @RequestBody TaxRequest request) {
        return ResponseEntity.ok(taxCalculationService.previewTax(request));
    }

    @Operation(summary = "Yearly tax summary",
            description = "Taxes of all sources for a calendar year (default: current year), per type and per month.")
    @GetMapping("/account/{accountId}/summary")
    public ResponseEntity<TaxSummaryResponse> getYearlyTaxSummary(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year) {

        return ResponseEntity.ok(taxCalculationService.getYearlyTaxSummary(accountId, userId,
                year == null ? Year.now().getValue() : year));
    }

    @Operation(summary = "Taxes charged to an account", description = "Newest first.")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<AccountTaxResponse>> getAccountTaxes(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(taxCalculationService.getAccountTaxes(accountId, userId));
    }
}
