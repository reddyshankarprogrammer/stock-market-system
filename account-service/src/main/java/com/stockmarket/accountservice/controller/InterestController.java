package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.request.InterestRequest;
import com.stockmarket.accountservice.dto.response.InterestResponse;
import com.stockmarket.accountservice.security.RequestUser;
import com.stockmarket.accountservice.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Interest on cash balances.
 *
 * Posting ({@code POST /api/v1/accounts/{id}/interest}) is internal: the api-gateway blocks it.
 */
@Tag(name = "Interest", description = "Interest paid on cash balances")
@RestController
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @Operation(summary = "Post interest (ADMIN)",
            description = "Simple interest on the current balance for the period. Requires X-User-Role ADMIN; "
                    + "the api-gateway only lets ADMIN tokens through.")
    @PostMapping("/api/v1/accounts/{id}/interest")
    public ResponseEntity<InterestResponse> postInterest(
            @PathVariable String id,
            @Valid @RequestBody InterestRequest request) {

        RequestUser.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(interestService.postInterest(id, request));
    }

    @Operation(summary = "Interest paid to an account", description = "Newest first.")
    @GetMapping("/api/v1/interest/account/{accountId}")
    public ResponseEntity<List<InterestResponse>> getInterest(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(interestService.getInterest(accountId, userId));
    }
}
