package com.stockmarket.accountservice.entity;

import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A trade on an investment account.
 *
 * <ul>
 *   <li>PLATFORM trades execute here at the market-data price and settle in cash.</li>
 *   <li>MANUAL and IMPORT trades record what happened at an external broker; they never move cash.</li>
 *   <li>EXECUTION: one BUY or SELL fill; changes holdings. CLOSED_POSITION: an opened-and-closed
 *       position with buy and sell price; only contributes realized P&amp;L.</li>
 *   <li>{@code grossAmount} = price x quantity; {@code feeAmount} = brokerage + otherFee.</li>
 *   <li>BUY: {@code netAmount} = gross + fees + taxes. SELL: gross - fees - taxes.
 *       CLOSED_POSITION: realizedProfit - taxes.</li>
 *   <li>{@code realizedProfit} (SELL and CLOSED_POSITION) is after fees, before taxes.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "account_trades",
        indexes = {
                @Index(name = "idx_trades_account_date", columnList = "account_id, trade_date"),
                @Index(name = "idx_trades_symbol", columnList = "symbol"),
                @Index(name = "idx_trades_user", columnList = "user_id"),
                @Index(name = "idx_trades_statement", columnList = "statement_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trades_account_external_id",
                columnNames = {"account_id", "external_trade_id"})
)
public class Trade extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    /**
     * Owner, copied from the account for per-user queries.
     */
    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String userId;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String symbol;

    /**
     * Exchange or venue, for example NSE, NASDAQ, BINANCE.
     */
    @Size(max = 20)
    @Column(length = 20)
    private String exchange;

    /**
     * BUY or SELL. For a CLOSED_POSITION: BUY = long position, SELL = short position.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private TradeSide side;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TradeKind tradeKind = TradeKind.EXECUTION;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RecordSource source = RecordSource.PLATFORM;

    /**
     * Shares, coins or lots; fractional for crypto and forex.
     */
    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    /**
     * Execution price; for a CLOSED_POSITION, the closing price.
     */
    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;

    /**
     * Purchase price: the price of a BUY, the average cost of a SELL, the buy side of a closed position.
     */
    @Positive
    @Column(precision = 19, scale = 8)
    private BigDecimal buyPrice;

    /**
     * Sale price: the price of a SELL, the sell side of a closed position; null for a BUY.
     */
    @Positive
    @Column(precision = 19, scale = 8)
    private BigDecimal sellPrice;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    /**
     * Broker commission part of {@code feeAmount}.
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal brokerage = BigDecimal.ZERO;

    /**
     * Every other fee part of {@code feeAmount} (platform, exchange, swap, ...).
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal otherFee = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    /**
     * SELL: (price - average cost) x quantity - fees. CLOSED_POSITION: (sell - buy) x quantity - fees.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal realizedProfit;

    /**
     * SELL: the holding's average price when the trade executed. CLOSED_POSITION: the buy price.
     */
    @Column(precision = 19, scale = 8)
    private BigDecimal averageCostAtTrade;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime tradeDate;

    /**
     * Statement the trade was imported from; null for PLATFORM and MANUAL trades.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id")
    private AccountStatement statement;

    /**
     * Broker's trade, order or ticket id; unique per account, so a trade is imported once.
     */
    @Size(max = 100)
    @Column(length = 100)
    private String externalTradeId;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;
}
