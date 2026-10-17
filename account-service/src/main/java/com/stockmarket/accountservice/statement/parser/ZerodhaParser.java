package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.BrokerName;
import org.springframework.stereotype.Component;

/**
 * Zerodha Console exports.
 *
 * Default layout ({@code account.import.brokers.zerodha}): the tradebook (one row per fill:
 * symbol, trade_type, quantity, price, order_execution_time, exchange, trade_id), plus the
 * charges and P&amp;L summary tables of the P&amp;L statement when present.
 */
@Component
public class ZerodhaParser extends AbstractConfigurableParser {

    public ZerodhaParser(BrokerImportProperties properties) {
        super(properties);
    }

    @Override
    public BrokerName broker() {
        return BrokerName.ZERODHA;
    }
}
