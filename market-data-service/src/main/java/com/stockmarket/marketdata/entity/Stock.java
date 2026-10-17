package com.stockmarket.marketdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

@Entity
@Table(name = "stocks", indexes = {
        @Index(name = "idx_stock_exchange", columnList = "exchange"),
        @Index(name = "idx_stock_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal initialPrice;

    @Column(nullable = false, length = 30)
    private String exchange; // e.g., "NASDAQ", "NSE", "LSE"

    @Column(nullable = false, length = 10)
    private String currency; // e.g., "USD", "INR", "GBP"

    // Timezone of the primary trading exchange (e.g., "America/New_York", "Asia/Kolkata")
    @Column(nullable = false, length = 50)
    private String timeZone;

    // Regular trading hours in local exchange time
    @Column(name = "market_open_time")
    private LocalTime marketOpenTime; // e.g., 09:30:00

    @Column(name = "market_close_time")
    private LocalTime marketCloseTime; // e.g., 16:00:00

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // UTC Timestamps
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public ZoneId getExchangeZoneId() {
        return ZoneId.of(this.timeZone);
    }
}
