# order-service

Market orders against the user-service wallet: price from market-data-service, settlement in user-service, outcome published to Kafka.

| | |
|---|---|
| Port | `ORDER_SERVICE_PORT` (8083) |
| Base package | `com.stockmarket.orderservice` |
| Spring Boot | 4.0.8, Spring Cloud 2025.1.3 (OpenFeign) |
| Database | MySQL, table `orders`, Hibernate `ddl-auto: update` |
| Calls | market-data-service (price), user-service (`funds/deduct`, `funds/credit`) via Feign |
| Publishes | Kafka `order.executed`, `order.failed` |

## Run and test

```powershell
.\mvnw.cmd spring-boot:run        # needs MySQL + Kafka; user-service and market-data-service for real orders
.\mvnw.cmd test                   # OrderServiceTest (Mockito) + contextLoads
```

## Package layout

```
com.stockmarket.orderservice
├── OrderServiceApplication             @EnableFeignClients
├── client/MarketDataClient             GET /api/v1/market-data/stocks/{symbol} -> StockPriceResponse
├── client/UserServiceClient            POST /api/v1/users/{id}/funds/deduct|credit?amount=
├── controller/OrderController          /api/v1/orders
├── dto/OrderRequest, StockPriceResponse
├── entity/Order, OrderStatus, OrderType
├── exception/                          OrderNotFound, OrderAccessDenied, OrderExecution + GlobalExceptionHandler
├── repository/OrderRepository          findByUserIdOrderByCreatedAtDesc
└── service/OrderService                placeOrder, getOrder, getMyOrders, publishOrderEvent
```

## Order flow (`OrderService.placeOrder`)

1. Upper-case the symbol; get the price from market-data-service (404 → `FeignException.NotFound` → 404).
2. `total = price × quantity`; save the order as **PENDING**.
3. BUY → `deductFunds`, SELL → `creditFunds` on user-service.
4. Success → **EXECUTED**, `executedAt`, publish `order.executed`. Failure → **FAILED** with `failureReason` (the `message` of user-service's error body), publish `order.failed`, throw `OrderExecutionException` (422 with `orderId`).

Deliberately **not** `@Transactional`: remote calls must not run inside a DB transaction; each save commits on its own.

## API (`/api/v1/orders`, token required)

| Method | Path | Behaviour |
|---|---|---|
| POST | `/` | `{symbol, type: BUY|SELL, quantity ≥ 1}` (`price` optional and ignored) → 201 Order |
| GET | `/my-orders` | Caller's orders, newest first |
| GET | `/{orderId}` | 404 unknown, 403 another user's order |

## Data model: `orders`

`id` (UUID), `user_id`, `symbol`, `type`, `status` (CREATED, PENDING, EXECUTED, FAILED, CANCELLED; only PENDING/EXECUTED/FAILED used), `quantity` (int), `price`, `total_amount`, `failure_reason`, `created_at`, `executed_at`. Indexes on user_id, symbol, status.

## Events

`order.executed` / `order.failed`, key orderId: `{orderId, userId, symbol, type, quantity, price, totalAmount, status, timestamp, reason?}`. Consumed by portfolio-service (`executed`) and notification-service (both).

## Configuration

| Key | Notes |
|---|---|
| `hosts.*` | mysql, kafka, user-service, market-data-service (docker profile overrides) |
| `services.user.url`, `services.market-data.url` | Feign base URLs |
| `kafka.topics.order-executed`, `kafka.topics.order-failed` | required (no defaults in code) |

## Known limitations

- SELL is not checked against holdings (portfolio-service): selling unowned shares credits the wallet.
- If funds were deducted and saving EXECUTED fails, there is no compensation (consider an outbox).
- Separate from account-service: these orders do not appear in investment accounts.
