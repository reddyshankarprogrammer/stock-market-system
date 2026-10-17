package com.stockmarket.portfolioservice.service;


import com.stockmarket.portfolioservice.client.MarketDataClient;
import com.stockmarket.portfolioservice.entity.Holding;
import com.stockmarket.portfolioservice.repository.HoldingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class PortfolioService {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private MarketDataClient marketDataClient;

    public Map<String, Object> getPortfolio(String userId) {

        List<Holding> holdings =
                holdingRepository.findByUserId(userId);

        List<Map<String, Object>> portfolioItems =
                new ArrayList<>();

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentPortfolioValue = BigDecimal.ZERO;
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        for (Holding holding : holdings) {

            Map<String, Object> stockData =
                    marketDataClient.getStock(holding.getSymbol());

            BigDecimal currentPrice =
                    new BigDecimal(stockData.get("price").toString());

            BigDecimal investedAmount =
                    holding.getTotalInvested();

            BigDecimal currentValue =
                    currentPrice.multiply(
                            BigDecimal.valueOf(
                                    holding.getQuantity()));

            BigDecimal profitLoss =
                    currentValue.subtract(investedAmount);

            BigDecimal profitLossPercentage =
                    investedAmount.compareTo(BigDecimal.ZERO) > 0
                            ? profitLoss.multiply(BigDecimal.valueOf(100))
                            .divide(
                                    investedAmount,
                                    2,
                                    RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

            totalInvested =
                    totalInvested.add(investedAmount);

            currentPortfolioValue =
                    currentPortfolioValue.add(currentValue);

            totalProfitLoss =
                    totalProfitLoss.add(profitLoss);

            Map<String, Object> item =
                    new HashMap<>();

            item.put("symbol", holding.getSymbol());
            item.put("companyName", holding.getCompanyName());
            item.put("quantity", holding.getQuantity());
            item.put("averageBuyPrice", holding.getAverageBuyPrice());

            item.put("currentPrice", currentPrice);
            item.put("investedAmount", investedAmount);
            item.put("currentValue", currentValue);

            item.put("profitLoss", profitLoss);
            item.put(
                    "profitLossPercentage",
                    profitLossPercentage);

            portfolioItems.add(item);
        }

        BigDecimal portfolioReturnPercentage =
                totalInvested.compareTo(BigDecimal.ZERO) > 0
                        ? totalProfitLoss.multiply(BigDecimal.valueOf(100))
                        .divide(
                                totalInvested,
                                2,
                                RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        Map<String, Object> response =
                new HashMap<>();

        response.put("userId", userId);

        // Summary
        response.put("totalHoldings", holdings.size());
        response.put("totalInvested", totalInvested);
        response.put("currentPortfolioValue", currentPortfolioValue);
        response.put("totalProfitLoss", totalProfitLoss);
        response.put(
                "portfolioReturnPercentage",
                portfolioReturnPercentage);

        // Holding Details
        response.put("holdings", portfolioItems);

        return response;
    }
}

