package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvestmentAccountRepository extends JpaRepository<InvestmentAccount, String> {

    List<InvestmentAccount> findByUserId(String userId);

    /**
     * The account only if it belongs to the user: ownership in a single query.
     */
    Optional<InvestmentAccount> findByUserIdAndId(String userId, String id);

    List<InvestmentAccount> findByUserIdAndStatus(String userId, AccountStatus status);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * Loads the account with a database row lock ({@code SELECT ... FOR UPDATE}).
     *
     * Every operation that changes the balance goes through this, so two concurrent
     * withdrawals or trades on the same account are serialized instead of both
     * reading the same starting balance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM InvestmentAccount a WHERE a.id = :id")
    Optional<InvestmentAccount> findByIdForUpdate(@Param("id") String id);
}
