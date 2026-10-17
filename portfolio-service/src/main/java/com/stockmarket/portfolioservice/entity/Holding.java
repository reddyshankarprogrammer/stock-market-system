package com.stockmarket.portfolioservice.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "holdings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "symbol"}
        )
)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String companyName;

    private int quantity;

    @Column(precision = 15, scale = 2)
    private BigDecimal averageBuyPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalInvested;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Holding() {
    }

    public Holding(String id,
                   String userId,
                   String symbol,
                   String companyName,
                   int quantity,
                   BigDecimal averageBuyPrice,
                   BigDecimal totalInvested,
                   LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.totalInvested = totalInvested;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAverageBuyPrice() {
        return averageBuyPrice;
    }

    public void setAverageBuyPrice(BigDecimal averageBuyPrice) {
        this.averageBuyPrice = averageBuyPrice;
    }

    public BigDecimal getTotalInvested() {
        return totalInvested;
    }

    public void setTotalInvested(BigDecimal totalInvested) {
        this.totalInvested = totalInvested;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
