package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, String> {

    List<Trade> findByAccountIdOrderByTradeDateDesc(String accountId);

    List<Trade> findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(
            String accountId, LocalDateTime from, LocalDateTime to);

    List<Trade> findBySymbol(String symbol);

    List<Trade> findByTradeDateBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Sum of realized profit (after trade fees) over SELL executions and closed positions.
     */
    @Query("""
            SELECT COALESCE(SUM(t.realizedProfit), 0)
            FROM Trade t
            WHERE t.account.id = :accountId
              AND t.realizedProfit IS NOT NULL
            """)
    BigDecimal sumRealizedProfitByAccountId(@Param("accountId") String accountId);

    /**
     * Fees already deducted inside realizedProfit (SELL executions and closed positions),
     * added back to get gross profit.
     */
    @Query("""
            SELECT COALESCE(SUM(t.feeAmount), 0)
            FROM Trade t
            WHERE t.account.id = :accountId
              AND (t.side = com.stockmarket.accountservice.enums.TradeSide.SELL
                   OR t.tradeKind = com.stockmarket.accountservice.enums.TradeKind.CLOSED_POSITION)
            """)
    BigDecimal sumFeesDeductedFromRealizedProfit(@Param("accountId") String accountId);

    /**
     * Executions of an account in the order they happened, for rebuilding holdings.
     */
    @Query("""
            SELECT t FROM Trade t
            WHERE t.account.id = :accountId
            ORDER BY t.tradeDate ASC, t.createdAt ASC
            """)
    List<Trade> findAllForReplay(@Param("accountId") String accountId);

    boolean existsByAccountIdAndExternalTradeId(String accountId, String externalTradeId);

    @Query("SELECT t.id FROM Trade t WHERE t.statement.id = :statementId")
    List<String> findIdsByStatementId(@Param("statementId") String statementId);

    @Modifying
    @Query("DELETE FROM Trade t WHERE t.statement.id = :statementId")
    int deleteByStatementId(@Param("statementId") String statementId);
}
