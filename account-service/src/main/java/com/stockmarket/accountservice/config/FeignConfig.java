package com.stockmarket.accountservice.config;

import com.stockmarket.accountservice.security.RequestUser;
import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign setup.
 *
 * Downstream services authorize on {@code X-User-Id} / {@code X-User-Role} (set by the
 * api-gateway), so every Feign call forwards the identity of the current request. Calls
 * made outside a request carry no identity and are treated as anonymous by user-service.
 */
@Configuration
@EnableFeignClients(basePackages = "com.stockmarket.accountservice.client")
public class FeignConfig {

    @Bean
    public RequestInterceptor userContextPropagationInterceptor() {
        return template -> {
            RequestUser.header(RequestUser.USER_ID_HEADER)
                    .ifPresent(userId -> template.header(RequestUser.USER_ID_HEADER, userId));
            RequestUser.header(RequestUser.USER_ROLE_HEADER)
                    .ifPresent(role -> template.header(RequestUser.USER_ROLE_HEADER, role));
        };
    }
}
