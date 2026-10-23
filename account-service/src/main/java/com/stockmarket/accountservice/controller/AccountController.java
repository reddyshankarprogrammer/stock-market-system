package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.request.CreateAccountRequest;
import com.stockmarket.accountservice.dto.request.UpdateAccountRequest;
import com.stockmarket.accountservice.dto.response.AccountResponse;
import com.stockmarket.accountservice.dto.response.AccountSummaryResponse;
import com.stockmarket.accountservice.dto.response.AuditLogResponse;
import com.stockmarket.accountservice.dto.response.PerformanceResponse;
import com.stockmarket.accountservice.service.AccountService;
import com.stockmarket.accountservice.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Investment account lifecycle.
 *
 * Base URL: /api/v1/accounts
 */
@Tag(name = "Accounts", description = "Open, view, rename and close investment accounts")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final PerformanceService performanceService;

    @Operation(summary = "Open an account", description = "Creates an ACTIVE account with a zero balance.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Valid @RequestBody CreateAccountRequest request) {

        AccountResponse account = accountService.createAccount(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(account.getId()).toUri();
        return ResponseEntity.created(location).body(account);
    }

    @Operation(summary = "Get an account")
    @ApiResponse(responseCode = "200", description = "The account")
    @ApiResponse(responseCode = "403", description = "Account belongs to another user")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(accountService.getAccount(id, userId));
    }

    @Operation(summary = "Rename an account")
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Valid @RequestBody UpdateAccountRequest request) {

        return ResponseEntity.ok(accountService.updateAccount(id, userId, request));
    }

    @Operation(summary = "Close an account",
            description = "Soft delete: the balance must be zero and no shares held. History stays readable.")
    @ApiResponse(responseCode = "200", description = "Account closed")
    @ApiResponse(responseCode = "400", description = "Balance not zero, shares still held, or account not active")
    @DeleteMapping("/{id}")
    public ResponseEntity<AccountResponse> deleteAccount(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(accountService.deleteAccount(id, userId));
    }

    @Operation(summary = "List a user's accounts",
            description = "The caller's own accounts; an ADMIN may list any existing user's accounts.")
    @ApiResponse(responseCode = "200", description = "The accounts, newest first")
    @ApiResponse(responseCode = "403", description = "userId is not the caller and the caller is not ADMIN")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getUserAccounts(
            @PathVariable String userId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String callerId) {

        return ResponseEntity.ok(accountService.getUserAccounts(userId, callerId));
    }

    @Operation(summary = "Account summary",
            description = "Cash, live value of holdings, realized/unrealized profit and lifetime totals.")
    @GetMapping("/{id}/summary")
    public ResponseEntity<AccountSummaryResponse> getSummary(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(accountService.getSummary(id, userId));
    }

    @Operation(summary = "Account performance",
            description = "Recalculates and returns gross profit, realized/unrealized profit, charges, taxes and "
                    + "net profit (netProfit = grossProfit - totalCharges - totalTaxes).")
    @GetMapping("/{id}/performance")
    public ResponseEntity<PerformanceResponse> getPerformance(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(performanceService.calculatePerformance(id, userId));
    }

    @Operation(summary = "Audit trail", description = "Every action on the account with request/response JSON, newest first.")
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditLogResponse>> getAuditTrail(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.status(HttpStatus.OK).body(accountService.getAuditTrail(id, userId));
    }
}
