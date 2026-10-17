package com.stockmarket.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Feign client for user-service wallet operations.
 */
@FeignClient(name = "user-service", url = "${services.user.url}")
public interface UserServiceClient {

    @PostMapping("/api/v1/users/{userId}/funds/deduct")
    void deductFunds(@PathVariable("userId") String userId,
                     @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/v1/users/{userId}/funds/credit")
    void creditFunds(@PathVariable("userId") String userId,
                     @RequestParam("amount") BigDecimal amount);
}
