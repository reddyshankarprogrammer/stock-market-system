package com.stockmarket.portfolioservice.repository;

import com.stockmarket.portfolioservice.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository
        extends JpaRepository<Holding, String> {

    Optional<Holding> findByUserIdAndSymbol(
            String userId,
            String symbol);

    List<Holding> findByUserId(String userId);
}
