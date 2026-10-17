package com.stockmarket.orderservice.client;

import com.stockmarket.orderservice.dto.StockPriceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for market-data-service.
 */
@FeignClient(name = "market-data-service", url = "${services.market-data.url}")
public interface MarketDataClient {

    @GetMapping("/api/v1/market-data/stocks/{symbol}")
    StockPriceResponse getStockPrice(@PathVariable("symbol") String symbol);
}
