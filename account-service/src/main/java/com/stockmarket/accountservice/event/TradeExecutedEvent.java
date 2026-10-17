package com.stockmarket.accountservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published (after commit) when a trade executes on an investment account.
 * Sent to Kafka topic {@code kafka.topics.trade-executed}, keyed by account id.
 */
public record TradeExecutedEvent(String tradeId,
                                 String accountId,
                                 String userId,
                                 String symbol,
                                 String side,
                                 int quantity,
                                 BigDecimal price,
                                 BigDecimal grossAmount,
                                 BigDecimal feeAmount,
                                 BigDecimal taxAmount,
                                 BigDecimal netAmount,
                                 BigDecimal realizedProfit,
                                 BigDecimal balanceAfter,
                                 LocalDateTime tradeDate) {
}
