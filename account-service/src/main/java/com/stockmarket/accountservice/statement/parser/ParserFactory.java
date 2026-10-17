package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.BrokerName;
import com.stockmarket.accountservice.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the {@link StatementParser} for a broker. Every parser bean registers itself through
 * {@link StatementParser#broker()}, so adding a broker is: a parser class plus its layout in
 * application.yaml.
 */
@Component
public class ParserFactory {

    private final Map<BrokerName, StatementParser> parsers = new EnumMap<>(BrokerName.class);

    public ParserFactory(List<StatementParser> parserBeans) {
        for (StatementParser parser : parserBeans) {
            StatementParser previous = parsers.put(parser.broker(), parser);
            if (previous != null) {
                throw new IllegalStateException("Two statement parsers for " + parser.broker() + ": "
                        + previous.getClass().getSimpleName() + " and " + parser.getClass().getSimpleName());
            }
        }
    }

    /**
     * @throws ValidationException if no parser exists for the broker
     */
    public StatementParser getParser(BrokerName broker) {
        StatementParser parser = parsers.get(broker);
        if (parser == null) {
            throw new ValidationException("Statement import is not supported for " + broker
                    + "; supported: " + parsers.keySet());
        }
        return parser;
    }

    public boolean supports(BrokerName broker) {
        return parsers.containsKey(broker);
    }
}
