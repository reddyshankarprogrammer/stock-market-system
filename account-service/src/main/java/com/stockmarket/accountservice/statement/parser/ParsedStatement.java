package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.ChargeType;
import com.stockmarket.accountservice.enums.TaxType;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeSide;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything a parser recognized in a statement, independent of the broker's layout.
 *
 * @param trades      trade rows
 * @param charges     statement-level charges (charge section), not per-trade
 * @param taxes       statement-level taxes (charge section labels mapped to taxes)
 * @param summary     the broker's own P&amp;L summary, if the file has one
 * @param warnings    skipped rows and unmapped labels, human readable
 * @param rowsRead    data rows looked at
 * @param rowsSkipped data rows that could not be parsed
 */
public record ParsedStatement(List<Trade> trades,
                              List<Charge> charges,
                              List<Tax> taxes,
                              PnlSummary summary,
                              List<String> warnings,
                              int rowsRead,
                              int rowsSkipped) {

    /**
     * One trade row.
     *
     * @param rowNumber   1-based row in the sheet, for messages
     * @param externalId  broker trade/order/ticket id, if the file has one
     * @param kind        EXECUTION or CLOSED_POSITION
     * @param side        BUY/SELL; for a closed position BUY = long, SELL = short
     * @param price       execution price (EXECUTION only)
     * @param buyPrice    buy price (CLOSED_POSITION)
     * @param sellPrice   sell price (CLOSED_POSITION)
     * @param tradeDate   execution date; closing date for a closed position
     * @param realizedPnl broker-reported P&amp;L before per-row charges (CLOSED_POSITION)
     * @param brokerage   per-row commission (positive)
     * @param otherFee    per-row other fees (positive)
     * @param tax         per-row tax (positive)
     */
    public record Trade(int rowNumber,
                        String externalId,
                        String symbol,
                        String exchange,
                        TradeKind kind,
                        TradeSide side,
                        BigDecimal quantity,
                        BigDecimal price,
                        BigDecimal buyPrice,
                        BigDecimal sellPrice,
                        LocalDateTime tradeDate,
                        BigDecimal realizedPnl,
                        BigDecimal brokerage,
                        BigDecimal otherFee,
                        BigDecimal tax) {
    }

    public record Charge(ChargeType type, BigDecimal amount, String label) {
    }

    public record Tax(TaxType type, BigDecimal amount, String label) {
    }

    /**
     * Broker-reported totals; any field may be null.
     */
    public record PnlSummary(BigDecimal realizedPnl,
                             BigDecimal unrealizedPnl,
                             BigDecimal charges,
                             BigDecimal taxes,
                             BigDecimal netPnl) {

        public static final PnlSummary EMPTY = new PnlSummary(null, null, null, null, null);
    }

    public LocalDate periodFrom() {
        return trades.stream().map(t -> t.tradeDate().toLocalDate()).min(LocalDate::compareTo).orElse(null);
    }

    public LocalDate periodTo() {
        return trades.stream().map(t -> t.tradeDate().toLocalDate()).max(LocalDate::compareTo).orElse(null);
    }
}
