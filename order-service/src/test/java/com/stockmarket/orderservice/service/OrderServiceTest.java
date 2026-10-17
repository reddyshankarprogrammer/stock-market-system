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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private MarketDataClient marketDataClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "orderExecutedTopic", "order.executed");
        ReflectionTestUtils.setField(orderService, "orderFailedTopic", "order.failed");
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId("order-1");
            }
            return order;
        });
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, Object>) null));
    }

    @Test
    void placeOrder_buy_deductsFundsAtMarketPriceAndPublishesExecuted() {
        when(marketDataClient.getStockPrice("AAPL"))
                .thenReturn(new StockPriceResponse("AAPL", new BigDecimal("200.00")));

        Order order = orderService.placeOrder(request(OrderType.BUY, " aapl ", 10));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(order.getSymbol()).isEqualTo("AAPL");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("2000.00");
        assertThat(order.getExecutedAt()).isNotNull();
        verify(userServiceClient).deductFunds("user-1", new BigDecimal("2000.00"));
        verify(kafkaTemplate).send(eq("order.executed"), eq("order-1"), any());
    }

    @Test
    void placeOrder_sell_creditsFunds() {
        when(marketDataClient.getStockPrice("TSLA"))
                .thenReturn(new StockPriceResponse("TSLA", new BigDecimal("100.00")));

        orderService.placeOrder(request(OrderType.SELL, "TSLA", 5));

        verify(userServiceClient).creditFunds("user-1", new BigDecimal("500.00"));
    }

    @Test
    void placeOrder_fundsFailure_marksFailedAndPublishesFailed() {
        when(marketDataClient.getStockPrice("AAPL"))
                .thenReturn(new StockPriceResponse("AAPL", new BigDecimal("200.00")));
        doThrow(new RuntimeException("Insufficient funds"))
                .when(userServiceClient).deductFunds(anyString(), any());

        assertThatThrownBy(() -> orderService.placeOrder(request(OrderType.BUY, "AAPL", 10)))
                .isInstanceOf(OrderExecutionException.class)
                .hasMessageContaining("Insufficient funds");

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(saved.getValue().getFailureReason()).isEqualTo("Insufficient funds");
        verify(kafkaTemplate).send(eq("order.failed"), eq("order-1"), any());
    }

    @Test
    void getOrder_otherUsersOrder_throwsAccessDenied() {
        Order order = new Order();
        order.setId("order-1");
        order.setUserId("someone-else");
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder("order-1", "user-1"))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void getOrder_missing_throwsNotFound() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder("missing", "user-1"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private OrderRequest request(OrderType type, String symbol, int quantity) {
        OrderRequest request = new OrderRequest();
        request.setUserId("user-1");
        request.setSymbol(symbol);
        request.setType(type);
        request.setQuantity(quantity);
        return request;
    }
}
