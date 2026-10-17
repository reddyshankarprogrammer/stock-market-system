package com.stockmarket.accountservice.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamWriteFeature;

/**
 * JSON settings for money values (Jackson 3, Spring Boot 4).
 *
 * BigDecimal amounts are written in plain notation ({@code 0.00001}, never {@code 1E-5}),
 * so clients that parse numbers as text get exactly what is stored.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer plainBigDecimalCustomizer() {
        return builder -> builder.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    }
}
