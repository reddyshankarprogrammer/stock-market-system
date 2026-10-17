package com.stockmarket.marketdata.service;

import com.stockmarket.marketdata.dto.StockPrice;
import com.stockmarket.marketdata.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private static final String PRICE_KEY_PREFIX = "stock_price:";

    private static final String PRICE_UPDATED_TOPIC = "stock.price.updated";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final StockRepository stockRepository;
    private final RedisTemplate<String, StockPrice> redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Live (simulated) prices keyed by symbol; the source of truth for reads. Redis holds a copy for other services.
    // Entries are replaced, never mutated, so readers always see a consistent snapshot.
    private final Map<String, StockPrice> stockPriceCache = new ConcurrentHashMap<>();


    @EventListener(ApplicationReadyEvent.class)
    public void initializePriceCache() {
        log.info("Initializing stock price cache...");
        stockRepository.findByActiveTrue().forEach(stock -> {
            BigDecimal initialPrice = stock.getInitialPrice();
            StockPrice stockPrice = new StockPrice(
                    stock.getSymbol(),
                    stock.getCompanyName(),
                    initialPrice,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    initialPrice,
                    initialPrice,
                    initialPrice,
                    0L,
                    LocalDateTime.now()
            );
            stockPriceCache.put(stock.getSymbol(), stockPrice);
        });
        log.info("Loaded {} stocks into price cache", stockPriceCache.size());
    }

    /**
     * Get the current price of a stock by its symbol.
     *
     * @param symbol The stock symbol.
     * @return The current price of the stock, or null if not found.
     */
    public StockPrice getStockPrice(String symbol) {
        return stockPriceCache.get(symbol);
    }

    public List<StockPrice> getAllPrices() {
        return List.copyOf(stockPriceCache.values());
    }


    @Scheduled(fixedRateString = "${market.price-update-interval}")
    public void updatePrices() {

        if (stockPriceCache.isEmpty()) {
            log.warn("No stocks loaded");
            return;
        }

        stockPriceCache.forEach((symbol, currentPrice) -> {
            // One failing symbol (e.g. Redis down) must not stop the rest of the tick
            try {
                StockPrice updated = nextPrice(currentPrice);
                stockPriceCache.put(symbol, updated);
                publish(updated);
            } catch (Exception e) {
                log.error("Failed to update price for {}", symbol, e);
            }
        });

        log.info("Stock prices updated for {} symbols", stockPriceCache.size());

    }

    private StockPrice nextPrice(StockPrice currentPrice) {
        BigDecimal oldPrice = currentPrice.getPrice();
        BigDecimal open = currentPrice.getOpen();

        // +- 0.25% price movement
        double priceMovement =
                (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5;

        BigDecimal priceChange = oldPrice
                .multiply(BigDecimal.valueOf(priceMovement / 100))
                .setScale(2, RoundingMode.HALF_UP);

        // Prevent negative prices
        BigDecimal newPrice = oldPrice
                .add(priceChange)
                .setScale(2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);

        BigDecimal changeAmount = newPrice.subtract(open)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal changePercent = open.signum() == 0
                ? BigDecimal.ZERO
                : changeAmount.divide(open, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);

        return new StockPrice(
                currentPrice.getSymbol(),
                currentPrice.getCompanyName(),
                newPrice,
                changeAmount,
                changePercent,
                currentPrice.getHigh().max(newPrice),
                currentPrice.getLow().min(newPrice),
                open,
                currentPrice.getVolume(),
                LocalDateTime.now()
        );
    }

    private void publish(StockPrice price) {
        String symbol = price.getSymbol();

        // 1. Cache the updated price in Redis
        redisTemplate.opsForValue().set(PRICE_KEY_PREFIX + symbol, price);

        // 2. Broadcast the updated price to WebSocket subscribers -- All Client
        simpMessagingTemplate.convertAndSend("/topic/price/" + symbol, price);

        // 3. Publish to Kafka
        Map<String, Object> priceUpdatedEvent = Map.of(
                "symbol", symbol,
                "price", price.getPrice(),
                "timestamp", price.getTimestamp().toString());

        kafkaTemplate.send(PRICE_UPDATED_TOPIC, symbol, priceUpdatedEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish price update for {} to Kafka", symbol, ex);
                    }
                });
    }

}
