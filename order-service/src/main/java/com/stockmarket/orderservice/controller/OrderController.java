package com.stockmarket.orderservice.controller;

import com.stockmarket.orderservice.dto.OrderRequest;
import com.stockmarket.orderservice.entity.Order;
import com.stockmarket.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller responsible for handling stock order operations.
 *
 * Available Operations:
 * - Place Buy/Sell Order
 * - Retrieve Order Details
 * - Retrieve User Orders
 *
 * Base URL:
 * /api/v1/orders
 *
 * The X-User-Id header is expected to be set by the API gateway
 * after JWT validation.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    /**
     * Service responsible for order processing
     * and business validation.
     */
    private final OrderService orderService;

    /**
     * Creates a new stock order at the current market price.
     *
     * The user id from the request header overrides any
     * userId supplied in the request payload to prevent
     * user impersonation.
     *
     * @param userId authenticated user identifier
     * @param request order request
     * @return created order
     */
    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody OrderRequest request) {

        log.info(
                "Received {} order request for symbol {} from user {}",
                request.getType(),
                request.getSymbol(),
                userId);

        // Override userId from authenticated request
        request.setUserId(userId);

        Order order = orderService.placeOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }

    /**
     * Retrieves all orders belonging to the authenticated user,
     * newest first.
     *
     * Example:
     * GET /api/v1/orders/my-orders
     *
     * Request Header:
     * X-User-Id: USER001
     *
     * @param userId authenticated user identifier
     * @return list of user orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestHeader("X-User-Id") String userId) {

        List<Order> orders = orderService.getMyOrders(userId);

        log.info(
                "Retrieved {} orders for user {}",
                orders.size(),
                userId);

        return ResponseEntity.ok(orders);
    }

    /**
     * Retrieves a specific order by its identifier.
     *
     * Responds 404 if the order does not exist and 403 if it
     * belongs to another user.
     *
     * @param orderId order identifier
     * @param userId authenticated user identifier
     * @return order details
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
            @PathVariable String orderId,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(orderService.getOrder(orderId, userId));
    }
}
