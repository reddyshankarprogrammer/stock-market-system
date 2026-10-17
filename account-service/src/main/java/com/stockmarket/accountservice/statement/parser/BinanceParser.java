package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.BrokerName;
import org.springframework.stereotype.Component;

/**
 * Binance spot trade history.
 *
 * Default layout ({@code account.import.brokers.binance}): Date(UTC), Pair, Side, Price,
 * Executed, Amount, Fee, one row per fill. Quantities such as {@code 0.5BTC} and fees such as
 * {@code 0.0001BNB} are read by their leading number; the fee is stored as brokerage.
 */
@Component
public class BinanceParser extends AbstractConfigurableParser {

    public BinanceParser(BrokerImportProperties properties) {
        super(properties);
    }

    @Override
    public BrokerName broker() {
        return BrokerName.BINANCE;
    }
}
