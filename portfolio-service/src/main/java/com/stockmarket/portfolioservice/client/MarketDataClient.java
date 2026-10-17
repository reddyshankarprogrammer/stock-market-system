package com.stockmarket.portfolioservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(
        name = "market-data-service",
        url = "${services.market-data.url}"
)
public interface MarketDataClient {

    @GetMapping("/api/v1/market-data/stocks/{symbol}")
    Map<String, Object> getStock(
            @PathVariable("symbol") String symbol);
}