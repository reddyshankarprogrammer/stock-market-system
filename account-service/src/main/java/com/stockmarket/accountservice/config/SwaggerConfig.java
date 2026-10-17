package com.stockmarket.accountservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI description served by springdoc.
 *
 * <ul>
 *   <li>Swagger UI: {@code http://localhost:8086/swagger-ui.html}</li>
 *   <li>OpenAPI JSON: {@code http://localhost:8086/v3/api-docs}</li>
 * </ul>
 * Swagger UI calls the service directly, so set the {@code X-User-Id} header yourself;
 * through the api-gateway it is filled from the JWT.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI accountServiceOpenApi(@Value("${server.port}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .version("v1")
                        .description("""
                                Investment accounts, cash ledger, trades, holdings, fees, taxes,
                                dividends and statements.

                                Every endpoint except the fee/tax schedules requires the X-User-Id
                                header, which the api-gateway sets from the validated JWT.
                                Dividend and interest posting are internal endpoints that the
                                gateway blocks."""))
                .servers(List.of(
                        new Server().url("http://localhost:" + port).description("Direct (set X-User-Id yourself)"),
                        new Server().url("http://localhost:8080").description("Through api-gateway (Bearer token)")));
    }
}
