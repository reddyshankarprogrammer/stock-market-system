package com.stockmarket.accountservice.controller;

import com.stockmarket.accountservice.dto.request.BuyTradeRequest;
import com.stockmarket.accountservice.dto.request.ManualTradeRequest;
import com.stockmarket.accountservice.dto.request.SellTradeRequest;
import com.stockmarket.accountservice.dto.response.TradeResponse;
import com.stockmarket.accountservice.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Trades, executed at the current market-data-service price.
 *
 * Base URL: /api/v1/trades
 */
@Tag(name = "Trades", description = "Buy and sell shares")
@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "Buy shares",
            description = "Debits price x quantity + fees + taxes and adds to the holding.")
    @ApiResponse(responseCode = "201", description = "Trade executed")
    @ApiResponse(responseCode = "404", description = "Unknown symbol or account")
    @ApiResponse(responseCode = "422", description = "Insufficient balance")
    @ApiResponse(responseCode = "502", description = "market-data-service unavailable")
    @PostMapping("/buy")
    public ResponseEntity<TradeResponse> buy(
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Valid @RequestBody BuyTradeRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.buyTrade(userId, request));
    }

    @Operation(summary = "Sell shares",
            description = "Credits price x quantity, debits fees and taxes (incl. capital gains tax on profit).")
    @ApiResponse(responseCode = "201", description = "Trade executed")
    @ApiResponse(responseCode = "400", description = "Not enough shares held")
    @PostMapping("/sell")
    public ResponseEntity<TradeResponse> sell(
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Valid @RequestBody SellTradeRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.sellTrade(userId, request));
    }

    @Operation(summary = "Record a manual trade (tracking accounts)",
            description = "Records a trade done at an external broker on a GROWW, ZERODHA, BINANCE, EXNESS or MANUAL "
                    + "account. Give buyPrice for a BUY, sellPrice for a SELL, or both for a closed position. "
                    + "Never moves cash; holdings are rebuilt.")
    @ApiResponse(responseCode = "201", description = "Trade recorded")
    @ApiResponse(responseCode = "400", description = "Platform account, or a missing price")
    @ApiResponse(responseCode = "409", description = "externalTradeId already recorded")
    @PostMapping
    public ResponseEntity<TradeResponse> createManualTrade(
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @Valid @RequestBody ManualTradeRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.createManualTrade(userId, request));
    }

    @Operation(summary = "Get a trade")
    @GetMapping("/{id}")
    public ResponseEntity<TradeResponse> getTrade(
            @PathVariable String id,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(tradeService.getTrade(id, userId));
    }

    @Operation(summary = "List an account's trades",
            description = "Newest first. Optional inclusive date range: give both from and to (yyyy-MM-dd).")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TradeResponse>> getTrades(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(tradeService.getTrades(accountId, userId, from, to));
    }
}
