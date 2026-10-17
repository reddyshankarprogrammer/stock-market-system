package com.stockmarket.marketdata.controller;

import com.stockmarket.marketdata.dto.StockPrice;
import com.stockmarket.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market-data")
@Slf4j
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;


    @RequestMapping("/stocks")
    public ResponseEntity<List<StockPrice>> getAllStock() {
        return ResponseEntity.ok(marketDataService.getAllPrices());
    }

    /**
     * Get the stock price for a specific symbol.
     *
     * @param symbol The stock symbol.
     * @return The stock price for the given symbol.
     */
    @RequestMapping("/stocks/{symbol}")
    public ResponseEntity<StockPrice> getStockPrice(@PathVariable String symbol) {
        StockPrice stockPrice = marketDataService.getStockPrice(symbol.toUpperCase());
        if (stockPrice != null) {
            return ResponseEntity.ok(stockPrice);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
/**
     * Get the list of available stock symbols.
     *
     * @return A list of available stock symbols.
     */
    @GetMapping("/stocks/list")
    public ResponseEntity<String> getStockList() {
        List<StockPrice> stockPrices = marketDataService.getAllPrices();
        StringBuilder response = new StringBuilder("Available Stocks:\n");
        for (StockPrice stockPrice : stockPrices) {
            response.append(stockPrice.getSymbol()).append(" - ").append(stockPrice.getCompanyName()).append("\n");
        }
        return ResponseEntity.ok(response.toString());
    }








}
