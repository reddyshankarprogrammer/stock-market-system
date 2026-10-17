package com.stockmarket.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Error handling for the Kafka listeners. Spring Boot wires this
 * CommonErrorHandler into the default listener container factory.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    /**
     * Retries a failed record twice, one second apart, then logs and skips it
     * so a bad event can't block the partition. Invalid events and
     * deserialization failures are skipped without retrying.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, ex) -> log.error(
                        "Skipping Kafka record. Topic={}, Partition={}, Offset={}, Value={}",
                        record.topic(), record.partition(), record.offset(), record.value(), ex),
                new FixedBackOff(1000L, 2L));

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }
}
