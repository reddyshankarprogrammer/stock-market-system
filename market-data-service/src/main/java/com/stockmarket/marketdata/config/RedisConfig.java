package com.stockmarket.marketdata.config;

import com.stockmarket.marketdata.dto.StockPrice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for stock market data caching.
 *
 * Stores the latest stock prices in Redis to provide:
 * - Fast read access for stock quotes
 * - Reduced database load
 * - Shared cache across multiple application instances
 *
 * Redis Key Example:
 *     stock:AAPL
 *
 * Redis Value Example:
 * {
 *     "symbol": "AAPL",
 *     "price": 195.30,
 *     "timestamp": "2026-10-12T10:30:15"
 * }
 */

@Configuration
public class RedisConfig {

    /**
     * Creates a RedisTemplate specialized for storing StockPrice objects.
     *
     * Key:
     *     String (e.g. stock:AAPL)
     *
     * Value:
     *     StockPrice serialized as JSON
     *
     * @param connectionFactory Redis connection factory
     * @return configured RedisTemplate
     */

    @Bean
    public RedisTemplate<String, StockPrice> stockPriceRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, StockPrice> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(StockPrice.class));
        return template;
    }

}
