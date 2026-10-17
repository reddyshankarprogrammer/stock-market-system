package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.BrokerName;
import com.stockmarket.accountservice.enums.TradeMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statement layouts per broker ({@code account.import.*} in application.yaml).
 *
 * Brokers change their export columns over time, so headers are configuration, not code:
 * each logical column lists the header names it may appear under (matched case-insensitively,
 * ignoring extra spaces). Charge and summary sections map row labels to charge/tax types.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "account.import")
public class BrokerImportProperties {

    /**
     * Data rows read per file at most; larger files are rejected.
     */
    private int maxRows = 100_000;

    /**
     * Rows scanned from the top of each sheet when looking for a header row
     * (statements often start with a preamble: client name, period, ...).
     */
    private int headerSearchRows = 50;

    private Map<BrokerName, BrokerFormat> brokers = new EnumMap<>(BrokerName.class);

    public BrokerFormat format(BrokerName broker) {
        BrokerFormat format = brokers.get(broker);
        if (format == null) {
            throw new StatementParseException("No statement format configured for " + broker
                    + " (account.import.brokers." + broker.name().toLowerCase() + ")");
        }
        return format;
    }

    /**
     * Layout of one broker's statement.
     */
    @Getter
    @Setter
    public static class BrokerFormat {

        /**
         * EXECUTION: one row per fill. CLOSED_POSITION: one row per round trip with P&amp;L.
         */
        private TradeMode tradeMode = TradeMode.EXECUTION;

        /**
         * Exchange stored on trades when the file has no exchange column.
         */
        private String defaultExchange;

        /**
         * Date patterns tried in order (java.time syntax); ISO formats are always tried too.
         */
        private List<String> dateFormats = new ArrayList<>();

        /**
         * Logical column -> header aliases. Keys: symbol, side, quantity, price, date, exchange, id,
         * buyprice, sellprice, openprice, closeprice, opendate, closedate, buydate, selldate, pnl,
         * brokerage, fee, tax.
         */
        private Map<String, List<String>> columns = new LinkedHashMap<>();

        private List<String> buyValues = new ArrayList<>(List.of("buy", "b"));

        private List<String> sellValues = new ArrayList<>(List.of("sell", "s"));

        /**
         * For per-row brokerage/fee columns that are signed (Exness commission and swap):
         * negative values are charges; positive values are income added to the P&amp;L.
         */
        private boolean signedRowCharges = false;

        /**
         * Statement-level charges: a two-column label/amount table.
         */
        private Section charges = new Section();

        /**
         * The broker's P&amp;L summary: a label/amount table.
         */
        private Section summary = new Section();
    }

    /**
     * A label/amount table. {@code labels} maps a label fragment (matched as "label contains
     * fragment", case-insensitive) to a target: {@code CHARGE:<ChargeType>} or {@code TAX:<TaxType>}
     * for the charge section, or REALIZED_PNL / UNREALIZED_PNL / CHARGES / TAXES / NET_PNL for the
     * summary section. Use {@code IGNORE} to skip a label without a warning.
     */
    @Getter
    @Setter
    public static class Section {

        private List<String> labelColumns = new ArrayList<>();

        private List<String> amountColumns = new ArrayList<>();

        private Map<String, String> labels = new LinkedHashMap<>();

        public boolean isConfigured() {
            return !labelColumns.isEmpty() && !amountColumns.isEmpty() && !labels.isEmpty();
        }
    }
}
