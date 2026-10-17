package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.request.FeeRequest;
import com.stockmarket.accountservice.dto.response.AccountFeeResponse;
import com.stockmarket.accountservice.dto.response.FeeBreakdownResponse;
import com.stockmarket.accountservice.dto.response.FeeMasterResponse;
import com.stockmarket.accountservice.service.FeeCalculationService;
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
 * Fee schedule, fee previews and fees charged.
 *
 * Base URL: /api/v1/fees
 */
@Tag(name = "Fees", description = "Fee schedule, previews and fees charged")
@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeCalculationService feeCalculationService;

    @Operation(summary = "Active fee schedule")
    @GetMapping
    public ResponseEntity<List<FeeMasterResponse>> getFeeSchedule() {
        return ResponseEntity.ok(feeCalculationService.getFeeSchedule());
    }

    @Operation(summary = "Preview fees", description = "What a trade or withdrawal of this amount would cost. Nothing is charged.")
    @PostMapping("/calculate")
    public ResponseEntity<FeeBreakdownResponse> previewFees(@Valid @RequestBody FeeRequest request) {
        return ResponseEntity.ok(feeCalculationService.previewFees(request));
    }

    @Operation(summary = "Fees charged to an account", description = "Newest first.")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<AccountFeeResponse>> getAccountFees(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(feeCalculationService.getAccountFees(accountId, userId));
    }
}
