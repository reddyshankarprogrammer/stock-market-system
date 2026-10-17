package com.stockmarket.accountservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards account events to Kafka once the database transaction has committed.
 *
 * Services publish events with Spring's {@code ApplicationEventPublisher} inside their
 * transaction; this listener only runs AFTER_COMMIT, so a rolled-back trade never
 * produces a Kafka message. A send failure is logged and does not affect the
 * already-committed transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.trade-executed}")
    private String tradeExecutedTopic;

    @Value("${kafka.topics.fund-transaction}")
    private String fundTransactionTopic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTradeExecuted(TradeExecutedEvent event) {
        send(tradeExecutedTopic, event.accountId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFundTransaction(FundTransactionEvent event) {
        send(fundTransactionTopic, event.accountId(), event);
    }

    private void send(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to {}", event.getClass().getSimpleName(), topic, ex);
                    } else {
                        log.debug("Published {} to {}", event.getClass().getSimpleName(), topic);
                    }
                });
    }
}
