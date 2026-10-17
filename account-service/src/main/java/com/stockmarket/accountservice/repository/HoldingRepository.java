package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
