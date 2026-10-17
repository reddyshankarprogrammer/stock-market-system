package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.ChargeType;
import com.stockmarket.accountservice.enums.StatementFormat;
import com.stockmarket.accountservice.enums.TaxType;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeSide;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parsers against the default layouts shipped in application.yaml.
 */
class StatementParsersTest {

    private static BrokerImportProperties properties;
    private final TabularDataReader reader = new TabularDataReader();

    @BeforeAll
    static void loadDefaultLayouts() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        List<PropertySource<?>> documents = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml"));
        documents.forEach(environment.getPropertySources()::addLast);
        properties = Binder.get(environment).bind("account.import", BrokerImportProperties.class).get();
    }

    @Test
    void zerodhaTradebookCsv() {
        String csv = """
                symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time
                INFY,INE009A01021,2026-10-01,NSE,EQ,EQ,buy,false,10,1500.50,1001,9001,2026-10-01T09:20:11
                INFY,INE009A01021,2026-10-03,NSE,EQ,EQ,sell,false,4,1550,1002,9002,2026-10-03T10:01:00
                TCS,INE467B01029,2026-10-03,BSE,EQ,EQ,hold,false,1,3600,1003,9003,2026-10-03T11:00:00
                """;

        ParsedStatement parsed = new ZerodhaParser(properties).parse(read(csv));

        assertThat(parsed.trades()).hasSize(2);
        ParsedStatement.Trade buy = parsed.trades().getFirst();
        assertThat(buy.symbol()).isEqualTo("INFY");
        assertThat(buy.side()).isEqualTo(TradeSide.BUY);
        assertThat(buy.kind()).isEqualTo(TradeKind.EXECUTION);
        assertThat(buy.quantity()).isEqualByComparingTo("10");
        assertThat(buy.price()).isEqualByComparingTo("1500.50");
        assertThat(buy.exchange()).isEqualTo("NSE");
        assertThat(buy.externalId()).isEqualTo("1001");
        assertThat(buy.tradeDate()).isEqualTo(LocalDateTime.of(2026, 10, 1, 9, 20, 11));
        // the unknown side is skipped with a warning naming the row
        assertThat(parsed.rowsSkipped()).isEqualTo(1);
        assertThat(parsed.warnings()).singleElement().asString().contains("Row 4").contains("unknown side 'hold'");
    }

    @Test
    void growwPnlWorkbookWithChargesAndSummary() throws IOException {
        byte[] xlsx = workbook(sheet -> {
            rows(sheet,
                    new Object[]{"Client: ABC123", null},
                    new Object[]{"P&L Statement for 01-04-2026 to 30-09-2026"},
                    new Object[]{},
                    new Object[]{"Stock name", "ISIN", "Quantity", "Buy date", "Buy price", "Sell date", "Sell price", "Realised P&L"},
                    new Object[]{"Reliance Industries", "INE002A01018", 5, "02-05-2026", 2400, "15-07-2026", 2550.5, 752.5},
                    new Object[]{"HDFCBANK", "INE040A01034", 10, "10-06-2026", 1650, "20-08-2026", "1,600.00", "(500.00)"},
                    new Object[]{},
                    new Object[]{"Charges", "Amount"},
                    new Object[]{"Brokerage", 40},
                    new Object[]{"STT", "₹ 12.30"},
                    new Object[]{"GST", 7.2},
                    new Object[]{"Stamp duty", 1.5},
                    new Object[]{"Something new", 3},
                    new Object[]{"Total", 64},
                    new Object[]{},
                    new Object[]{"Summary", "Amount"},
                    new Object[]{"Realised P&L", 252.5},
                    new Object[]{"Charges", 64});
        });

        ParsedStatement parsed = new GrowwParser(properties).parse(reader.read(new ByteArrayInputStream(xlsx), StatementFormat.XLSX, 1000));

        assertThat(parsed.trades()).hasSize(2);
        ParsedStatement.Trade first = parsed.trades().getFirst();
        assertThat(first.kind()).isEqualTo(TradeKind.CLOSED_POSITION);
        assertThat(first.symbol()).isEqualTo("RELIANCEINDUSTRIES");
        assertThat(first.buyPrice()).isEqualByComparingTo("2400");
        assertThat(first.sellPrice()).isEqualByComparingTo("2550.5");
        assertThat(first.realizedPnl()).isEqualByComparingTo("752.5");
        assertThat(first.exchange()).isEqualTo("NSE");
        assertThat(parsed.trades().get(1).realizedPnl()).isEqualByComparingTo("-500");

        assertThat(parsed.charges()).extracting(ParsedStatement.Charge::type)
                .containsExactlyInAnyOrder(ChargeType.BROKERAGE, ChargeType.STT, ChargeType.OTHER);
        assertThat(parsed.charges()).filteredOn(c -> c.type() == ChargeType.STT)
                .singleElement().extracting(ParsedStatement.Charge::amount).isEqualTo(new BigDecimal("12.30"));
        assertThat(parsed.taxes()).singleElement().satisfies(tax -> {
            assertThat(tax.type()).isEqualTo(TaxType.GST);
            assertThat(tax.amount()).isEqualByComparingTo("7.2");
        });
        assertThat(parsed.warnings()).anyMatch(w -> w.contains("unmapped charge label 'Something new'"));
        assertThat(parsed.summary().realizedPnl()).isEqualByComparingTo("252.5");
        assertThat(parsed.summary().charges()).isEqualByComparingTo("64");
    }

    @Test
    void binanceQuantitiesAndFeesWithAssetSuffix() {
        String csv = """
                Date(UTC),Pair,Side,Price,Executed,Amount,Fee
                2026-09-30 14:02:11,BTCUSDT,BUY,63250.10,0.01500000BTC,948.75150000USDT,0.00001500BTC
                2026-10-02 08:00:00,BTCUSDT,SELL,64000,0.005BTC,320USDT,0.32USDT
                """;

        ParsedStatement parsed = new BinanceParser(properties).parse(read(csv));

        assertThat(parsed.trades()).hasSize(2);
        ParsedStatement.Trade buy = parsed.trades().getFirst();
        assertThat(buy.quantity()).isEqualByComparingTo("0.015");
        assertThat(buy.brokerage()).isEqualByComparingTo("0.000015");
        assertThat(buy.exchange()).isEqualTo("BINANCE");
        assertThat(parsed.trades().get(1).side()).isEqualTo(TradeSide.SELL);
    }

    @Test
    void exnessClosedPositionsLongAndShortWithSignedCharges() {
        String csv = """
                ticket,opening_time_utc,closing_time_utc,type,lots,symbol,opening_price,closing_price,commission,swap,profit
                5001,2026-10-01 08:00:00,2026-10-01 12:00:00,buy,0.10,EURUSD,1.0850,1.0900,-0.70,-0.25,50.00
                5002,2026-10-02 09:00:00,2026-10-02 10:30:00,sell,0.20,XAUUSD,2400.00,2390.00,-1.40,0.35,200.00
                """;

        ParsedStatement parsed = new ExnessParser(properties).parse(read(csv));

        ParsedStatement.Trade longTrade = parsed.trades().getFirst();
        assertThat(longTrade.kind()).isEqualTo(TradeKind.CLOSED_POSITION);
        assertThat(longTrade.side()).isEqualTo(TradeSide.BUY);
        assertThat(longTrade.buyPrice()).isEqualByComparingTo("1.0850");
        assertThat(longTrade.sellPrice()).isEqualByComparingTo("1.0900");
        assertThat(longTrade.brokerage()).isEqualByComparingTo("0.70");
        assertThat(longTrade.otherFee()).isEqualByComparingTo("0.25");
        assertThat(longTrade.realizedPnl()).isEqualByComparingTo("50.00");
        assertThat(longTrade.tradeDate()).isEqualTo(LocalDateTime.of(2026, 10, 1, 12, 0));

        ParsedStatement.Trade shortTrade = parsed.trades().get(1);
        assertThat(shortTrade.side()).isEqualTo(TradeSide.SELL);
        // short: sold at open, bought back at close
        assertThat(shortTrade.sellPrice()).isEqualByComparingTo("2400");
        assertThat(shortTrade.buyPrice()).isEqualByComparingTo("2390");
        // positive swap is income, not a charge
        assertThat(shortTrade.otherFee()).isEqualByComparingTo("0");
        assertThat(shortTrade.realizedPnl()).isEqualByComparingTo("200.35");
    }

    @Test
    void semicolonDelimitedCsvIsDetected() {
        String csv = "symbol;trade_type;quantity;price;order_execution_time\nSBIN;buy;3;600;2026-10-01 10:00:00\n";

        assertThat(new ZerodhaParser(properties).parse(read(csv)).trades()).hasSize(1);
    }

    @Test
    void fileWithoutRecognizableTableFails() {
        String csv = "foo,bar\n1,2\n";

        assertThatThrownBy(() -> new ZerodhaParser(properties).parse(read(csv)))
                .isInstanceOf(StatementParseException.class)
                .hasMessageContaining("No ZERODHA trade table found");
    }

    @Test
    void cellValuesParseLenientNumbersAndDates() {
        assertThat(CellValues.number("₹1,23,456.78")).isEqualByComparingTo("123456.78");
        assertThat(CellValues.number("(12.30)")).isEqualByComparingTo("-12.30");
        assertThat(CellValues.number("-0.70 USD")).isEqualByComparingTo("-0.70");
        assertThat(CellValues.number("  ")).isNull();
        assertThatThrownBy(() -> CellValues.number("n/a")).isInstanceOf(IllegalArgumentException.class);
        assertThat(CellValues.dateTime("15 Jul 2026", List.of("dd MMM yyyy"))).isEqualTo(LocalDateTime.of(2026, 7, 15, 0, 0));
    }

    private TabularData read(String csv) {
        return reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), StatementFormat.CSV, 1000);
    }

    private static byte[] workbook(java.util.function.Consumer<Sheet> filler) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            filler.accept(workbook.createSheet("P&L"));
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void rows(Sheet sheet, Object[]... rows) {
        for (int r = 0; r < rows.length; r++) {
            Row row = sheet.createRow(r);
            for (int c = 0; c < rows[r].length; c++) {
                Object value = rows[r][c];
                if (value instanceof Number number) {
                    row.createCell(c).setCellValue(number.doubleValue());
                } else if (value != null) {
                    row.createCell(c).setCellValue(value.toString());
                }
            }
        }
    }
}
