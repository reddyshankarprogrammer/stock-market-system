package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.request.BuyTradeRequest;
import com.stockmarket.accountservice.dto.request.ManualTradeRequest;
import com.stockmarket.accountservice.dto.request.SellTradeRequest;
import com.stockmarket.accountservice.dto.response.TradeResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Trades.
 *
 * <ul>
 *   <li>PLATFORM and PAPER accounts: {@link #buyTrade} / {@link #sellTrade} execute at the current
 *       market-data price and settle through the cash ledger.</li>
 *   <li>Tracking accounts (Groww, Zerodha, Binance, Exness, Manual): {@link #createManualTrade}
 *       records an external trade; it never moves cash.</li>
 * </ul>
 */
public interface TradeService {

    /**
     * BUY: validate balance, debit gross + fees + taxes, record the trade, update holdings.
     */
    TradeResponse buyTrade(String userId, BuyTradeRequest request);

    /**
     * SELL: validate holdings, credit gross, debit fees + taxes (including capital gains
     * tax on realized profit), record the trade, update holdings.
     */
    TradeResponse sellTrade(String userId, SellTradeRequest request);

    /**
     * Records an external trade on a tracking account: an EXECUTION (buyPrice or sellPrice)
     * or a CLOSED_POSITION (both). Stores its brokerage/fee as charges and its tax as a tax,
     * then rebuilds the account's holdings.
     */
    TradeResponse createManualTrade(String userId, ManualTradeRequest request);

    /**
     * One trade, if the caller may read its account.
     */
    TradeResponse getTrade(String tradeId, String userId);

    /**
     * Trades of an account, newest first. {@code from}/{@code to} are optional
     * inclusive day bounds; both must be given to filter.
     */
    List<TradeResponse> getTrades(String accountId, String userId, LocalDate from, LocalDate to);
}
