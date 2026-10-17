# portfolio-service

Holdings and live P&L built from executed orders (order-service).

| | |
|---|---|
| Port | `PORTFOLIO_SERVICE_PORT` (8084) |
| Base package | `com.stockmarket.portfolioservice` |
| Spring Boot | 4.0.8, Spring Cloud 2025.1.3 (OpenFeign) |
| Database | MySQL, table `holdings`, Hibernate `ddl-auto: update` |
| Consumes | Kafka `order.executed` (group `portfolio-service-group`) |
| Calls | market-data-service (Feign) for live prices |

## Run and test

```powershell
.\mvnw.cmd spring-boot:run        # needs MySQL, Kafka, market-data-service
.\mvnw.cmd test                   # contextLoads; needs docker-compose infrastructure
```

## Package layout

```
com.stockmarket.portfolioservice
├── PortfolioServiceApplication           @EnableFeignClients
├── client/MarketDataClient               GET /api/v1/market-data/stocks/{symbol} -> Map
├── controller/PortfolioController        /api/v1/portfolio
├── dto/OrderEvent                        (unused; the consumer reads a Map)
├── entity/Holding                        plain getters/setters (no Lombok)
├── kafka/PortfolioEventConsumer          @KafkaListener on order.executed
├── repository/HoldingRepository          findByUserIdAndSymbol, findByUserId
└── service/PortfolioService              getPortfolio (live prices, P&L)
```

## Holding updates (`PortfolioEventConsumer`)

| Event `type` | Change |
|---|---|
| BUY | Create or add: `avg = (oldAvg × oldQty + price × qty) / newQty` (2 decimals); `totalInvested += price × qty` |
| SELL | `qty -= sold`; delete the row at 0; `totalInvested = avg × remaining`. Selling more than held throws (logged) |

Exceptions are caught and logged inside the listener, so a failed update is **not retried**.

## API (`/api/v1/portfolio`)

| Method | Path | Behaviour |
|---|---|---|
| GET | `/me` | Caller's portfolio: per holding `quantity, averageBuyPrice, currentPrice, investedAmount, currentValue, profitLoss, profitLossPercentage`; totals `totalInvested, currentPortfolioValue, totalProfitLoss, portfolioReturnPercentage` |
| GET | `/{userId}` | Same for any user. Meant as internal, but **reachable through the gateway without an ownership check** |

## Data model: `holdings`

`id`, `user_id` (unique with `symbol`), `symbol`, `company_name` (currently the symbol), `quantity` (int), `average_buy_price`, `total_invested`, `updated_at`.

## Configuration

| Key | Notes |
|---|---|
| `hosts.*` | mysql, kafka, market-data-service |
| `services.market-data.url` | Feign base URL |
| `kafka.topics.order-executed` | consumed topic |
| `spring.kafka.consumer.*` | `ErrorHandlingDeserializer` → `JacksonJsonDeserializer` into `HashMap`, type headers ignored |

## Known limitations

- `GET /{userId}` exposes any user's portfolio to any logged-in user (add an owner/ADMIN check or block it at the gateway).
- Failed updates are swallowed (no `DefaultErrorHandler`, no dead-letter topic).
- `companyName` is set to the symbol.
- One market-data call per holding on every read.
