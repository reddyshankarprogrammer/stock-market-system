package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.AccountPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountPerformanceRepository extends JpaRepository<AccountPerformance, String> {

    Optional<AccountPerformance> findByAccountId(String accountId);
}
