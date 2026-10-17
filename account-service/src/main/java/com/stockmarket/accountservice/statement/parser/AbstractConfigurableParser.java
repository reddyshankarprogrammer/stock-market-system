package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.ChargeType;
import com.stockmarket.accountservice.enums.TaxType;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeMode;
import com.stockmarket.accountservice.enums.TradeSide;
import com.stockmarket.accountservice.statement.parser.BrokerImportProperties.BrokerFormat;
import com.stockmarket.accountservice.statement.parser.BrokerImportProperties.Section;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parser driven entirely by a broker's {@link BrokerFormat} configuration.
 *
 * <ol>
 *   <li>Find the trade table: the first row (in the first {@code headerSearchRows} rows of any
 *       sheet) whose cells contain every required column header; data rows follow it until the
 *       first blank row.</li>
 *   <li>Parse each data row; a row that fails is skipped with a warning naming row and reason.</li>
 *   <li>Find the charge and summary tables (label/amount) the same way and map their labels.</li>
 * </ol>
 * Broker parsers only declare which broker they are; layouts live in application.yaml.
 */
@RequiredArgsConstructor
public abstract class AbstractConfigurableParser implements StatementParser {

    private static final Set<String> SUMMARY_TARGETS = Set.of("REALIZED_PNL", "UNREALIZED_PNL", "CHARGES", "TAXES", "NET_PNL");
    private static final String IGNORE = "IGNORE";

    protected final BrokerImportProperties properties;

    @Override
    public ParsedStatement parse(TabularData data) {

        BrokerFormat format = properties.format(broker());
        List<String> warnings = new ArrayList<>();
        List<ParsedStatement.Trade> trades = new ArrayList<>();
        int rowsRead = 0;
        int skipped = 0;

        Optional<Table> tradeTable = findTable(data, format.getColumns(), requiredTradeColumns(format));
        if (tradeTable.isPresent()) {
            Table table = tradeTable.get();
            for (Table.DataRow row : table.rows()) {
                rowsRead++;
                try {
                    trades.add(format.getTradeMode() == TradeMode.CLOSED_POSITION
                            ? closedPosition(table, row, format)
                            : execution(table, row, format));
                } catch (IllegalArgumentException e) {
                    skipped++;
                    warnings.add("Row " + row.number() + " (" + table.sheet() + "): skipped, " + e.getMessage());
                }
            }
        }

        List<ParsedStatement.Charge> charges = new ArrayList<>();
        List<ParsedStatement.Tax> taxes = new ArrayList<>();
        Map<String, BigDecimal> summaryValues = new HashMap<>();
        rowsRead += readSection(data, format.getCharges(), warnings, (target, amount, label) -> {
            if (target.startsWith("CHARGE:")) {
                charges.add(new ParsedStatement.Charge(ChargeType.valueOf(target.substring(7)), amount.abs(), label));
            } else if (target.startsWith("TAX:")) {
                taxes.add(new ParsedStatement.Tax(TaxType.valueOf(target.substring(4)), amount.abs(), label));
            }
        }, true);
        rowsRead += readSection(data, format.getSummary(), warnings,
                (target, amount, label) -> summaryValues.putIfAbsent(target, amount), false);

        if (tradeTable.isEmpty() && charges.isEmpty() && taxes.isEmpty()) {
            throw new StatementParseException("No " + broker() + " trade table found; expected columns "
                    + describeRequired(format) + " in the first " + properties.getHeaderSearchRows() + " rows of a sheet");
        }

        ParsedStatement.PnlSummary summary = summaryValues.isEmpty()
                ? ParsedStatement.PnlSummary.EMPTY
                : new ParsedStatement.PnlSummary(summaryValues.get("REALIZED_PNL"), summaryValues.get("UNREALIZED_PNL"),
                summaryValues.get("CHARGES"), summaryValues.get("TAXES"), summaryValues.get("NET_PNL"));

        return new ParsedStatement(trades, charges, taxes, summary, warnings, rowsRead, skipped);
    }

    // ------------------------------------------------------------------ trade rows

    private ParsedStatement.Trade execution(Table table, Table.DataRow row, BrokerFormat format) {

        TradeSide side = side(required(table, row, "side"), format);
        BigDecimal quantity = positive(table, row, "quantity");
        BigDecimal price = positive(table, row, "price");
        LocalDateTime date = date(table, row, format, "date");
        RowCharges rowCharges = rowCharges(table, row, format);

        return new ParsedStatement.Trade(row.number(), optional(table, row, "id"), symbol(table, row),
                exchange(table, row, format), TradeKind.EXECUTION, side, quantity, price, null, null, date,
                null, rowCharges.brokerage(), rowCharges.otherFee(), rowCharges.tax());
    }

    private ParsedStatement.Trade closedPosition(Table table, Table.DataRow row, BrokerFormat format) {

        BigDecimal quantity = positive(table, row, "quantity");
        TradeSide side = table.has("side") ? side(required(table, row, "side"), format) : TradeSide.BUY;

        BigDecimal buy;
        BigDecimal sell;
        if (table.has("buyprice") && table.has("sellprice")) {
            buy = positive(table, row, "buyprice");
            sell = positive(table, row, "sellprice");
        } else {
            BigDecimal open = positive(table, row, "openprice");
            BigDecimal close = positive(table, row, "closeprice");
            // Long: bought at open, sold at close. Short: sold at open, bought back at close.
            buy = side == TradeSide.BUY ? open : close;
            sell = side == TradeSide.BUY ? close : open;
        }

        String dateColumn = firstPresent(table, "selldate", "closedate", "date");
        LocalDateTime closedAt = date(table, row, format, dateColumn);
        RowCharges rowCharges = rowCharges(table, row, format);

        BigDecimal pnl = table.has("pnl") && !CellValues.isBlank(row.cell(table.index("pnl")))
                ? CellValues.number(row.cell(table.index("pnl")))
                : sell.subtract(buy).multiply(quantity);

        return new ParsedStatement.Trade(row.number(), optional(table, row, "id"), symbol(table, row),
                exchange(table, row, format), TradeKind.CLOSED_POSITION, side, quantity, null, buy, sell, closedAt,
                pnl.add(rowCharges.income()), rowCharges.brokerage(), rowCharges.otherFee(), rowCharges.tax());
    }

    private record RowCharges(BigDecimal brokerage, BigDecimal otherFee, BigDecimal tax, BigDecimal income) {
    }

    /**
     * Per-row brokerage/fee/tax. With {@code signedRowCharges}, negative values are charges
     * and positive values are income (for example a positive swap).
     */
    private RowCharges rowCharges(Table table, Table.DataRow row, BrokerFormat format) {

        BigDecimal brokerage = BigDecimal.ZERO;
        BigDecimal otherFee = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;

        for (String column : List.of("brokerage", "fee")) {
            BigDecimal value = table.has(column) ? CellValues.number(row.cell(table.index(column))) : null;
            if (value == null || value.signum() == 0) {
                continue;
            }
            BigDecimal charge;
            if (format.isSignedRowCharges() && value.signum() > 0) {
                income = income.add(value);
                continue;
            } else {
                charge = value.abs();
            }
            if (column.equals("brokerage")) {
                brokerage = brokerage.add(charge);
            } else {
                otherFee = otherFee.add(charge);
            }
        }

        BigDecimal tax = table.has("tax") ? CellValues.number(row.cell(table.index("tax"))) : null;
        return new RowCharges(brokerage, otherFee, tax == null ? BigDecimal.ZERO : tax.abs(), income);
    }

    private String symbol(Table table, Table.DataRow row) {
        String symbol = required(table, row, "symbol").trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (symbol.length() > 20) {
            throw new IllegalArgumentException("symbol '" + symbol + "' is longer than 20 characters");
        }
        return symbol;
    }

    private String exchange(Table table, Table.DataRow row, BrokerFormat format) {
        String exchange = optional(table, row, "exchange");
        String value = exchange != null ? exchange.trim().toUpperCase(Locale.ROOT) : format.getDefaultExchange();
        return value == null ? null : value.substring(0, Math.min(value.length(), 20));
    }

    private TradeSide side(String text, BrokerFormat format) {
        String value = CellValues.normalize(text);
        if (format.getBuyValues().stream().map(CellValues::normalize).anyMatch(value::equals)) {
            return TradeSide.BUY;
        }
        if (format.getSellValues().stream().map(CellValues::normalize).anyMatch(value::equals)) {
            return TradeSide.SELL;
        }
        throw new IllegalArgumentException("unknown side '" + text + "'");
    }

    private BigDecimal positive(Table table, Table.DataRow row, String column) {
        BigDecimal value = CellValues.number(required(table, row, column));
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(column + " must be positive, got " + value.toPlainString());
        }
        return value;
    }

    private LocalDateTime date(Table table, Table.DataRow row, BrokerFormat format, String column) {
        return CellValues.dateTime(required(table, row, column), format.getDateFormats());
    }

    private static String required(Table table, Table.DataRow row, String column) {
        String value = table.has(column) ? row.cell(table.index(column)) : null;
        if (CellValues.isBlank(value)) {
            throw new IllegalArgumentException(column + " is empty");
        }
        return value;
    }

    private static String optional(Table table, Table.DataRow row, String column) {
        String value = table.has(column) ? row.cell(table.index(column)) : null;
        return CellValues.isBlank(value) ? null : value.trim();
    }

    private static String firstPresent(Table table, String... columns) {
        for (String column : columns) {
            if (table.has(column)) {
                return column;
            }
        }
        return columns[columns.length - 1];
    }

    private static List<String> requiredTradeColumns(BrokerFormat format) {
        if (format.getTradeMode() == TradeMode.EXECUTION) {
            return List.of("symbol", "side", "quantity", "price", "date");
        }
        return format.getColumns().containsKey("buyprice")
                ? List.of("symbol", "quantity", "buyprice", "sellprice")
                : List.of("symbol", "quantity", "side", "openprice", "closeprice");
    }

    private static String describeRequired(BrokerFormat format) {
        Map<String, List<String>> described = new LinkedHashMap<>();
        for (String column : requiredTradeColumns(format)) {
            described.put(column, format.getColumns().getOrDefault(column, List.of(column)));
        }
        return described.toString();
    }

    // ------------------------------------------------------------------ label/amount sections

    @FunctionalInterface
    private interface SectionConsumer {
        void accept(String target, BigDecimal amount, String label);
    }

    /**
     * Reads a label/amount table and passes each mapped row on.
     *
     * @return rows read
     */
    private int readSection(TabularData data, Section section, List<String> warnings,
                            SectionConsumer consumer, boolean warnUnmapped) {

        if (!section.isConfigured()) {
            return 0;
        }
        Map<String, List<String>> columns = Map.of("label", section.getLabelColumns(), "amount", section.getAmountColumns());
        Optional<Table> found = findTable(data, columns, List.of("label", "amount"));
        if (found.isEmpty()) {
            return 0;
        }

        Table table = found.get();
        int read = 0;
        for (Table.DataRow row : table.rows()) {
            String label = row.cell(table.index("label"));
            String amountText = row.cell(table.index("amount"));
            if (CellValues.isBlank(label) || CellValues.isBlank(amountText)) {
                continue;
            }
            read++;
            String target = target(section, label);
            BigDecimal amount;
            try {
                amount = CellValues.number(amountText);
            } catch (IllegalArgumentException e) {
                warnings.add("Row " + row.number() + " (" + table.sheet() + "): '" + label + "' skipped, " + e.getMessage());
                continue;
            }
            if (target == null) {
                if (warnUnmapped && amount.signum() != 0 && !isSummaryLabel(label)) {
                    warnings.add("Row " + row.number() + " (" + table.sheet() + "): unmapped charge label '" + label.trim()
                            + "' (" + amount.toPlainString() + ") not imported");
                }
            } else if (!IGNORE.equals(target) && (warnUnmapped || SUMMARY_TARGETS.contains(target))) {
                consumer.accept(target, amount, label.trim());
            }
        }
        return read;
    }

    private boolean isSummaryLabel(String label) {
        return target(properties.format(broker()).getSummary(), label) != null;
    }

    /**
     * The target of the longest configured fragment contained in the label, so
     * "integrated gst" wins over "gst" when both are configured.
     */
    private static String target(Section section, String label) {
        String normalized = CellValues.normalize(label);
        String bestTarget = null;
        int bestLength = -1;
        for (Map.Entry<String, String> entry : section.getLabels().entrySet()) {
            String fragment = CellValues.normalize(entry.getKey());
            if (!fragment.isEmpty() && normalized.contains(fragment) && fragment.length() > bestLength) {
                bestTarget = entry.getValue().trim().toUpperCase(Locale.ROOT);
                bestLength = fragment.length();
            }
        }
        return bestTarget;
    }

    // ------------------------------------------------------------------ table detection

    /**
     * A header row and the data rows under it (up to the first blank row).
     */
    private record Table(String sheet, Map<String, Integer> columns, List<DataRow> rows) {

        record DataRow(int number, List<String> cells) {
            String cell(int index) {
                return index < cells.size() ? cells.get(index) : null;
            }
        }

        boolean has(String column) {
            return columns.containsKey(column);
        }

        int index(String column) {
            return columns.get(column);
        }
    }

    private Optional<Table> findTable(TabularData data, Map<String, List<String>> aliases, List<String> required) {

        for (TabularData.Sheet sheet : data.sheets()) {
            List<List<String>> rows = sheet.rows();
            int limit = Math.min(rows.size(), properties.getHeaderSearchRows());

            for (int r = 0; r < limit; r++) {
                Map<String, Integer> columns = matchHeader(rows.get(r), aliases);
                if (!columns.keySet().containsAll(required)) {
                    continue;
                }
                List<Table.DataRow> dataRows = new ArrayList<>();
                for (int d = r + 1; d < rows.size(); d++) {
                    List<String> cells = rows.get(d);
                    if (cells.stream().allMatch(CellValues::isBlank)) {
                        break;
                    }
                    dataRows.add(new Table.DataRow(d + 1, cells));
                }
                return Optional.of(new Table(sheet.name(), columns, dataRows));
            }
        }
        return Optional.empty();
    }

    /**
     * Logical column -> cell index, for every configured column found in the header row.
     * Aliases are tried in their configured order, so the first alias wins when a file has
     * several candidate columns (Zerodha has both order_execution_time and trade_date).
     */
    private static Map<String, Integer> matchHeader(List<String> header, Map<String, List<String>> aliases) {

        List<String> cells = header.stream().map(CellValues::normalize).toList();
        Map<String, Integer> columns = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : aliases.entrySet()) {
            for (String alias : entry.getValue()) {
                int index = cells.indexOf(CellValues.normalize(alias));
                if (index >= 0) {
                    columns.put(entry.getKey(), index);
                    break;
                }
            }
        }
        return columns;
    }
}
