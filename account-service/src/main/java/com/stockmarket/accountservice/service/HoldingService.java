package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.response.HoldingResponse;
import com.stockmarket.accountservice.entity.Holding;
import com.stockmarket.accountservice.entity.InvestmentAccount;

import java.math.BigDecimal;
import java.util.List;

/**
 * Positions held in investment accounts.
 *
 * PLATFORM accounts update holdings incrementally as trades execute. Tracking accounts
 * rebuild them from their full trade history ({@link #rebuildHoldings}) whenever trades
 * are recorded, imported or deleted, so out-of-order imports still give the right result.
 */
public interface HoldingService {

    /**
     * BUY: adds units and recomputes the weighted average price.
     */
    Holding updateHoldingAfterBuy(InvestmentAccount account, String symbol, BigDecimal quantity, BigDecimal price);

    /**
     * SELL: removes units and accumulates realized profit. The average price of the
     * remaining units does not change.
     */
    Holding updateHoldingAfterSell(Holding holding, BigDecimal quantity, BigDecimal realizedProfit);

    /**
     * Weighted average price after adding units:
     * {@code (currentQty x currentAvg + addQty x addPrice) / (currentQty + addQty)}, 8 decimals.
     */
    BigDecimal calculateAveragePrice(BigDecimal currentQuantity, BigDecimal currentAverage,
                                     BigDecimal addQuantity, BigDecimal addPrice);

    /**
     * Replays every trade of a tracking account in date order and rewrites its holdings
     * (and the realized profit of its SELL executions).
     *
     * @return warnings, for example a SELL of more units than were held
     */
    List<String> rebuildHoldings(InvestmentAccount account);

    /**
     * Open positions of an account the caller may read, valued at live prices; the stored
     * valuation snapshot is refreshed when a live price is available.
     */
    List<HoldingResponse> getHoldings(String accountId, String userId);

    /**
     * Open positions valued at live prices (or the last known price), without an ownership check.
     */
    List<HoldingResponse> valueOpenPositions(String accountId);
}
