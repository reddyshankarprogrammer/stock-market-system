package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.response.LedgerResponse;
import com.stockmarket.accountservice.dto.response.StatementResponse;
import com.stockmarket.accountservice.service.LedgerService;
import com.stockmarket.accountservice.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Statements and the raw ledger.
 *
 * Base URL: /api/v1/accounts/{id}
 */
@Tag(name = "Statements", description = "Account statements and ledger with running balance")
@RestController
@RequestMapping("/api/v1/accounts/{id}")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;
    private final LedgerService ledgerService;

    @Operation(summary = "Account statement",
            description = "Opening/closing balance, ledger with running balance, trades, deposits/withdrawals, "
                    + "dividends, interest and fee/tax totals for an inclusive date range (max 366 days).")
    @GetMapping("/statement")
    public ResponseEntity<StatementResponse> getStatement(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Parameter(example = "2026-10-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-10-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(statementService.generateStatement(id, userId, from, to));
    }

    @Operation(summary = "Ledger with running balance", description = "Inclusive date range, in ledger order.")
    @GetMapping("/ledger")
    public ResponseEntity<List<LedgerResponse>> getLedger(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(ledgerService.getLedger(id, userId, from, to));
    }
}
