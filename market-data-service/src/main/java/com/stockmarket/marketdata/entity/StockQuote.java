package com.stockmarket.marketdata.entity;
/**
 * Represents a stock quote with its price, volume, and timestamp.
 * This entity is used to store historical stock quotes in the database.
 */
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_quotes", indexes = {
        @Index(name = "idx_quote_symbol_time", columnList = "symbol, timestamp DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class StockQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false)
    private Instant timestamp; // Always store UTC Instant
}