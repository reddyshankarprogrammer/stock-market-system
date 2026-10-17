package com.stockmarket.orderservice.service;

import com.stockmarket.orderservice.client.MarketDataClient;
import com.stockmarket.orderservice.client.UserServiceClient;
import com.stockmarket.orderservice.dto.OrderRequest;
import com.stockmarket.orderservice.dto.StockPriceResponse;
import com.stockmarket.orderservice.entity.Order;
import com.stockmarket.orderservice.entity.OrderStatus;
import com.stockmarket.orderservice.entity.OrderType;
import com.stockmarket.orderservice.exception.OrderAccessDeniedException;
import com.stockmarket.orderservice.exception.OrderExecutionException;
import com.stockmarket.orderservice.exception.OrderNotFoundException;
import com.stockmarket.orderservice.repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for order processing and management.
 *
 * Responsibilities:
 * - Price orders at the current market price
 * - Settle funds with user-service
 * - Persist orders and publish order events
 * - Retrieve order details and user order history
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    @Value("${kafka.topics.order-executed}")
    private String orderExecutedTopic;

    @Value("${kafka.topics.order-failed}")
    private String orderFailedTopic;

    private final MarketDataClient marketDataClient;
    private final UserServiceClient userServiceClient;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Places a BUY or SELL market order.
     *
     * FLOW:
     * 1. Get current price from market-data-service.
     * 2. Calculate total amount and save the order as PENDING.
     * 3. Deduct (BUY) or credit (SELL) funds via user-service.
     * 4. Mark EXECUTED or FAILED and publish the matching Kafka event.
     *
     * @param request order request (userId already set from the authenticated header)
     * @return executed order
     * @throws OrderExecutionException if funds settlement fails; the order is saved as FAILED
     */
    public Order placeOrder(OrderRequest request) {

        String symbol = request.getSymbol().trim().toUpperCase();

        log.info("Placing {} order: {} x {} for user: {}",
                request.getType(),
                request.getQuantity(),
                symbol,
                request.getUserId());

        // Step 1 : Get current market price
        BigDecimal currentPrice = fetchCurrentPrice(symbol);

        // Step 2 : Calculate total amount and persist as PENDING
        BigDecimal totalAmount = currentPrice.multiply(
                BigDecimal.valueOf(request.getQuantity()));

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setSymbol(symbol);
        order.setType(request.getType());
        order.setQuantity(request.getQuantity());
        order.setPrice(currentPrice);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        log.info("Order saved as PENDING. OrderId={}", savedOrder.getId());

        // Step 3 : Settle funds with user-service
        try {
            if (request.getType() == OrderType.BUY) {
                userServiceClient.deductFunds(request.getUserId(), totalAmount);
            } else {
                // TODO: verify holdings with portfolio-service before crediting a SELL
                userServiceClient.creditFunds(request.getUserId(), totalAmount);
            }
        } catch (Exception e) {
            String reason = describeFailure(e);

            log.error("Order execution failed. OrderId={}, Reason={}",
                    savedOrder.getId(), reason, e);

            savedOrder.setStatus(OrderStatus.FAILED);
            savedOrder.setFailureReason(reason);
            orderRepository.save(savedOrder);

            publishOrderEvent(orderFailedTopic, savedOrder);

            throw new OrderExecutionException(
                    savedOrder.getId(), "Order execution failed: " + reason, e);
        }

        // Step 4 : Mark executed and publish event
        savedOrder.setStatus(OrderStatus.EXECUTED);
        savedOrder.setExecutedAt(LocalDateTime.now());
        Order executedOrder = orderRepository.save(savedOrder);

        log.info("Order EXECUTED: {} {} x {} at {}. OrderId={}",
                request.getType(),
                request.getQuantity(),
                symbol,
                currentPrice,
                executedOrder.getId());

        publishOrderEvent(orderExecutedTopic, executedOrder);

        return executedOrder;
    }

    /**
     * Retrieves an order owned by the given user.
     *
     * @param orderId order identifier
     * @param userId  authenticated user identifier
     * @return order
     * @throws OrderNotFoundException     if the order does not exist
     * @throws OrderAccessDeniedException if the order belongs to another user
     */
    public Order getOrder(String orderId, String userId) {

        log.info("Fetching order {} for user {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            log.warn("Unauthorized access attempt. User {} tried to access order {}",
                    userId, orderId);
            throw new OrderAccessDeniedException(orderId);
        }

        return order;
    }

    /**
     * Retrieves all orders belonging to a user, newest first.
     *
     * @param userId user identifier
     * @return list of orders
     */
    public List<Order> getMyOrders(String userId) {

        log.info("Fetching orders for user {}", userId);

        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private BigDecimal fetchCurrentPrice(String symbol) {

        StockPriceResponse stockPrice = marketDataClient.getStockPrice(symbol);

        if (stockPrice == null || stockPrice.getPrice() == null
                || stockPrice.getPrice().signum() <= 0) {
            throw new IllegalStateException("No valid market price for symbol " + symbol);
        }

        return stockPrice.getPrice();
    }

    private String describeFailure(Exception e) {

        if (e instanceof FeignException fe && !fe.contentUTF8().isBlank()) {
            // user-service error bodies look like {"message":"Insufficient wallet balance",...}
            Matcher matcher = ERROR_MESSAGE.matcher(fe.contentUTF8());
            return matcher.find() ? matcher.group(1) : fe.contentUTF8();
        }

        return e.getMessage();
    }

    private void publishOrderEvent(String topic, Order order) {

        Map<String, Object> orderEvent = new HashMap<>();

        orderEvent.put("orderId", order.getId());
        orderEvent.put("userId", order.getUserId());
        orderEvent.put("symbol", order.getSymbol());
        orderEvent.put("type", order.getType().name());
        orderEvent.put("quantity", order.getQuantity());
        orderEvent.put("price", order.getPrice());
        orderEvent.put("totalAmount", order.getTotalAmount());
        orderEvent.put("status", order.getStatus().name());
        orderEvent.put("timestamp", LocalDateTime.now().toString());

        if (order.getFailureReason() != null) {
            orderEvent.put("reason", order.getFailureReason());
        }

        kafkaTemplate.send(topic, order.getId(), orderEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order event. Topic={}, OrderId={}",
                                topic, order.getId(), ex);
                    } else {
                        log.info("Order event published. Topic={}, OrderId={}, Status={}",
                                topic, order.getId(), order.getStatus());
                    }
                });
    }
}
