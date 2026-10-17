package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.request.DividendRequest;
import com.stockmarket.accountservice.dto.response.DividendResponse;
import com.stockmarket.accountservice.security.RequestUser;
import com.stockmarket.accountservice.service.DividendService;
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
 * Dividends.
 *
 * Posting ({@code POST /api/v1/accounts/{id}/dividends}) is internal: the api-gateway
 * blocks it, so only back-office or batch jobs on the internal network can call it.
 */
@Tag(name = "Dividends", description = "Dividends paid on held shares")
@RestController
@RequiredArgsConstructor
public class DividendController {

    private final DividendService dividendService;

    @Operation(summary = "Post a dividend (ADMIN)",
            description = "Pays dividendPerShare on the shares currently held, minus withholding tax. "
                    + "Requires X-User-Role ADMIN; the api-gateway only lets ADMIN tokens through.")
    @PostMapping("/api/v1/accounts/{id}/dividends")
    public ResponseEntity<DividendResponse> postDividend(
            @PathVariable String id,
            @Valid @RequestBody DividendRequest request) {

        RequestUser.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(dividendService.postDividend(id, request));
    }

    @Operation(summary = "Dividends of an account", description = "Newest first.")
    @GetMapping("/api/v1/dividends/account/{accountId}")
    public ResponseEntity<List<DividendResponse>> getDividends(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(dividendService.getDividends(accountId, userId));
    }
}
