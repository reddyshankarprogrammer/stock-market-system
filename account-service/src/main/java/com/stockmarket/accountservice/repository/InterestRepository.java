package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.InterestTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface InterestRepository extends JpaRepository<InterestTransaction, String> {

    List<InterestTransaction> findByAccountIdOrderByPostedAtDesc(String accountId);

    List<InterestTransaction> findByAccountIdAndPostedAtBetweenOrderByPostedAtAsc(
            String accountId, LocalDateTime from, LocalDateTime to);

    /**
     * True when interest was already posted for a period overlapping [start, end],
     * used to prevent paying the same days twice.
     */
    @Query("""
            SELECT COUNT(i) > 0
            FROM InterestTransaction i
            WHERE i.account.id = :accountId
              AND i.periodStart <= :end
              AND i.periodEnd >= :start
            """)
    boolean existsOverlappingPeriod(@Param("accountId") String accountId,
                                    @Param("start") LocalDate start,
                                    @Param("end") LocalDate end);

    @Query("""
            SELECT COALESCE(SUM(i.interestAmount), 0)
            FROM InterestTransaction i
            WHERE i.account.id = :accountId
            """)
    BigDecimal sumInterestByAccountId(@Param("accountId") String accountId);
}
