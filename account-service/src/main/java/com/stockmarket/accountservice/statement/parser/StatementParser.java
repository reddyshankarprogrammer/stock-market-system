package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.BrokerName;

/**
 * Turns a broker's statement into broker-independent {@link ParsedStatement} data.
 *
 * Implementations are Spring beans; {@link ParserFactory} picks one by {@link #broker()}.
 * A parser must not touch the database: the importer persists what it returns.
 */
public interface StatementParser {

    /**
     * Broker whose statements this parser reads.
     */
    BrokerName broker();

    /**
     * @throws StatementParseException if nothing in the file matches this broker's layout
     */
    ParsedStatement parse(TabularData data);
}
