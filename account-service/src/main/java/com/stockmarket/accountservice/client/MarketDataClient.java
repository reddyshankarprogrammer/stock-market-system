package com.stockmarket.accountservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stockmarket.accountservice.exception.MarketDataUnavailableException;
import com.stockmarket.accountservice.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * Reads current prices from market-data-service ({@code GET /api/v1/market-data/stocks/{symbol}}).
 *
 * Uses Spring's {@link RestClient} rather than OpenFeign, so account-service does not
 * need Spring Cloud.
 */
@Slf4j
@Component
public class MarketDataClient {

    private final RestClient restClient;

    public MarketDataClient(@Value("${services.market-data.url}") String baseUrl,
                            @Value("${services.market-data.connect-timeout:2s}") Duration connectTimeout,
                            @Value("${services.market-data.read-timeout:3s}") Duration readTimeout) {

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Current price of a symbol, for executing trades.
     *
     * @throws ResourceNotFoundException      if market-data-service does not know the symbol
     * @throws MarketDataUnavailableException if the service is down or returns no price
     */
    public BigDecimal getCurrentPrice(String symbol) {

        StockPrice stockPrice;
        try {
            stockPrice = restClient.get()
                    .uri("/api/v1/market-data/stocks/{symbol}", symbol)
                    .retrieve()
                    .body(StockPrice.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new ResourceNotFoundException("Stock", symbol);
            }
            throw new MarketDataUnavailableException(symbol, e);
        } catch (RestClientException e) {
            throw new MarketDataUnavailableException(symbol, e);
        }

        if (stockPrice == null || stockPrice.price() == null || stockPrice.price().signum() <= 0) {
            throw new MarketDataUnavailableException(symbol, null);
        }
        return stockPrice.price();
    }

    /**
     * Current price for valuations, where a missing price must not fail the request.
     *
     * @return the price, or empty when it cannot be fetched
     */
    public Optional<BigDecimal> findCurrentPrice(String symbol) {
        try {
            return Optional.of(getCurrentPrice(symbol));
        } catch (RuntimeException e) {
            log.debug("No live price for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * The fields of market-data-service's StockPrice that account-service uses.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record StockPrice(String symbol, BigDecimal price) {
    }
}
