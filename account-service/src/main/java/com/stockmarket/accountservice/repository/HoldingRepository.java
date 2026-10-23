package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, String> {

    Optional<Holding> findByAccountIdAndSymbol(String accountId, String symbol);

    List<Holding> findByAccountId(String accountId);

    List<Holding> findBySymbol(String symbol);

    /**
     * Holdings that still have shares, ordered by symbol.
     */
    @Query("""
            SELECT h FROM Holding h
            WHERE h.account.id = :accountId
              AND h.quantity > 0
            ORDER BY h.symbol
            """)
    List<Holding> findOpenPositions(@Param("accountId") String accountId);

    boolean existsByAccountIdAndQuantityGreaterThan(String accountId, java.math.BigDecimal quantity);

    /**
     * Stores the last known price of a position without the optimistic-lock check (it does not touch the version
     * column): the price is a cache that is overwritten on every valuation, so two overlapping reads must not
     * fail each other with a 409.
     */
    @Modifying
    @Query("""
            UPDATE Holding h
               SET h.currentPrice = :price, h.marketValue = :value, h.unrealizedPnl = :pnl, h.priceUpdatedAt = :at
             WHERE h.id = :id
            """)
    int updatePriceSnapshot(@Param("id") String id, @Param("price") BigDecimal price, @Param("value") BigDecimal value,
                            @Param("pnl") BigDecimal pnl, @Param("at") LocalDateTime at);
}
