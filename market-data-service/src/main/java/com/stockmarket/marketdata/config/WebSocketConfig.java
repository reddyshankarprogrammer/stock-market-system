package com.stockmarket.marketdata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration for Real-Time Stock Price Streaming.
 * <p>
 * How it works:
 * 1. Client establishes a WebSocket connection to /ws.
 * 2. Client subscribes to stock-specific topics:
 *    /topic/price/AAPL
 *    /topic/price/TSLA
 *    /topic/price/MSFT
 * 3. Stock price updates are generated and published by the server.
 * 4. Server pushes updates to all subscribed clients instantly.
 * 5. Clients receive price updates without REST polling.
 * <p>
 * Benefits:
 * - Real-time market data delivery
 * - Reduced API calls and server load
 * - Scalable publish/subscribe architecture
 * - Better user experience for trading dashboards
 * <p>
 * Example Flow:
 * <p>
 * Browser ---> WebSocket (/ws)
 *   |
 *   v
 * STOMP Message Broker
 *   |
 *   v
 * /topic/price/AAPL
 *   |
 *   v
 * Live price updates pushed to UI
 * <p>
 * Similar architecture is commonly used in stock trading,
 * cryptocurrency exchanges, and live market monitoring systems.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker responsible for routing
     * WebSocket messages between publishers and subscribers.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /**
         * Enables an in-memory message broker.
         *
         * Topics starting with "/topic" are treated as
         * publish-subscribe channels.
         *
         * Example:
         *   /topic/price/AAPL
         *   /topic/price/GOOG
         */
        registry.enableSimpleBroker("/topic");

        /**
         * Prefix for messages sent from client to server.
         *
         * Example:
         * Client sends:
         *   /app/order/buy
         *   /app/order/sell
         *
         * Server-side controllers can handle these destinations.
         */
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers WebSocket connection endpoints.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        /**
         * Main WebSocket endpoint.
         *
         * Client connects using:
         *   ws://localhost:8080/ws
         *
         * Allows connections from all origins.
         * Restrict origins in production environments.
         */
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");

        /**
         * SockJS fallback endpoint.
         *
         * Used when browsers or network environments
         * do not support native WebSockets.
         *
         * Client example:
         *   const socket = new SockJS('/ws');
         */
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
