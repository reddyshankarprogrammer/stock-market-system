package com.stockmarket.portfolioservice.controller;


import com.stockmarket.portfolioservice.entity.Holding;
import com.stockmarket.portfolioservice.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    /**
     * Get my portfolio with live P & L
     *
     * @param userId
     * @return
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyPortfolio(
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(
                portfolioService.getPortfolio(userId));
    }

    /**
     * Get portfolio by userId - For internal calls
     *
     * @param userId
     * @return
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getPortfolio(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                portfolioService.getPortfolio(userId));
    }
}