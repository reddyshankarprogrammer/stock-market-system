package com.stockmarket.accountservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published (after commit) when cash is deposited or withdrawn.
 * Sent to Kafka topic {@code kafka.topics.fund-transaction}, keyed by account id.
 */
public record FundTransactionEvent(String transactionId,
                                   String accountId,
                                   String userId,
                                   String transactionType,
                                   BigDecimal amount,
                                   BigDecimal feeAmount,
                                   BigDecimal balanceAfter,
                                   LocalDateTime transactionDate) {
}
