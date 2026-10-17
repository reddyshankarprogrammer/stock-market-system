package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.request.AdjustmentRequest;
import com.stockmarket.accountservice.dto.request.DepositRequest;
import com.stockmarket.accountservice.dto.request.WithdrawRequest;
import com.stockmarket.accountservice.dto.response.FundTransactionResponse;
import com.stockmarket.accountservice.service.FundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * Cash deposits and withdrawals.
 *
 * Base URL: /api/v1/accounts/{id}
 */
@Tag(name = "Funds", description = "Deposit and withdraw cash")
@RestController
@RequestMapping("/api/v1/accounts/{id}")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;

    @Operation(summary = "Deposit cash")
    @ApiResponse(responseCode = "201", description = "Deposit posted")
    @PostMapping("/deposit")
    public ResponseEntity<FundTransactionResponse> deposit(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Valid @RequestBody DepositRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(fundService.deposit(id, userId, request));
    }

    @Operation(summary = "Withdraw cash", description = "The withdrawal fee is charged on top of the amount.")
    @ApiResponse(responseCode = "201", description = "Withdrawal posted")
    @ApiResponse(responseCode = "422", description = "Balance does not cover amount + fee")
    @PostMapping("/withdraw")
    public ResponseEntity<FundTransactionResponse> withdraw(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Valid @RequestBody WithdrawRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(fundService.withdraw(id, userId, request));
    }

    @Operation(summary = "Adjust cash (statement reconciliation)",
            description = "Positive amount adds cash, negative removes it; posted as a STATEMENT_ADJUSTMENT ledger entry.")
    @ApiResponse(responseCode = "201", description = "Adjustment posted")
    @ApiResponse(responseCode = "422", description = "A negative adjustment larger than the balance")
    @PostMapping("/adjustments")
    public ResponseEntity<FundTransactionResponse> adjust(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Valid @RequestBody AdjustmentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(fundService.adjust(id, userId, request));
    }

    @Operation(summary = "List deposits, withdrawals and adjustments", description = "Newest first.")
    @GetMapping("/fund-transactions")
    public ResponseEntity<List<FundTransactionResponse>> getFundTransactions(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(fundService.getFundTransactions(id, userId));
    }
}
