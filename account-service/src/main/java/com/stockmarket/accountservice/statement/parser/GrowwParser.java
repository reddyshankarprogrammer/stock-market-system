package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.BrokerName;
import org.springframework.stereotype.Component;

/**
 * Groww stocks P&amp;L report.
 *
 * Default layout ({@code account.import.brokers.groww}): one row per closed lot with buy date,
 * buy price, sell date, sell price, quantity and realised P&amp;L, plus a charges table.
 */
@Component
public class GrowwParser extends AbstractConfigurableParser {

    public GrowwParser(BrokerImportProperties properties) {
        super(properties);
    }

    @Override
    public BrokerName broker() {
        return BrokerName.GROWW;
    }
}
