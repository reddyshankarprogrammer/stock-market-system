package com.stockmarket.accountservice.config;

import com.stockmarket.accountservice.statement.parser.BrokerImportProperties;
import com.stockmarket.accountservice.statement.storage.StatementStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the statement storage and broker import layouts from application.yaml.
 */
@Configuration
@EnableConfigurationProperties({StatementStorageProperties.class, BrokerImportProperties.class})
public class StatementConfig {
}
