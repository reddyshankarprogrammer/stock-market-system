package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.BrokerName;
import org.springframework.stereotype.Component;

/**
 * Exness closed positions report.
 *
 * Default layout ({@code account.import.brokers.exness}): one row per closed position with
 * ticket, symbol, type (buy/sell), lots, opening/closing price and time, commission, swap
 * and profit. Commission and swap are signed: negative values are charges, a positive swap
 * is added to the realized P&amp;L.
 */
@Component
public class ExnessParser extends AbstractConfigurableParser {

    public ExnessParser(BrokerImportProperties properties) {
        super(properties);
    }

    @Override
    public BrokerName broker() {
        return BrokerName.EXNESS;
    }
}
