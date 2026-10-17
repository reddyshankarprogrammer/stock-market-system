package com.stockmarket.marketdata.config;

import com.stockmarket.marketdata.entity.Stock;
import com.stockmarket.marketdata.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeds the stocks table with a default set of NASDAQ stocks when it is empty.
 *
 * Runs before ApplicationReadyEvent, so MarketDataService loads these into its
 * price cache on the first start.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;

    @Override
    public void run(String... args) {

        if (stockRepository.count() > 0) {
            log.info("Stocks table already populated, skipping seed");
            return;
        }

        List<Stock> stocks = List.of(
                nasdaq("AAPL", "Apple Inc.", "195.30"),
                nasdaq("MSFT", "Microsoft Corporation", "420.50"),
                nasdaq("GOOG", "Alphabet Inc.", "175.20"),
                nasdaq("AMZN", "Amazon.com Inc.", "185.60"),
                nasdaq("TSLA", "Tesla Inc.", "250.10"),
                nasdaq("NVDA", "NVIDIA Corporation", "120.80"));

        stockRepository.saveAll(stocks);

        log.info("Seeded {} stocks", stocks.size());
    }

    private Stock nasdaq(String symbol, String companyName, String initialPrice) {

        return Stock.builder()
                .symbol(symbol)
                .companyName(companyName)
                .initialPrice(new BigDecimal(initialPrice))
                .exchange("NASDAQ")
                .currency("USD")
                .timeZone("America/New_York")
                .marketOpenTime(LocalTime.of(9, 30))
                .marketCloseTime(LocalTime.of(16, 0))
                .build();
    }
}
