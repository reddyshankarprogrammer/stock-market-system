# market-data-service

Simulated live stock prices: REST, WebSocket (STOMP), Redis and Kafka.

| | |
|---|---|
| Port | `MARKET_DATA_SERVICE_PORT` (8082) |
| Base package | `com.stockmarket.marketdata` (not `marketdataservice`) |
| Spring Boot | 4.0.8 (Jackson 3) |
| Database | MySQL, table `stocks` (+ unused `stock_quotes`), Hibernate `ddl-auto: update` |
| Publishes | Kafka `stock.price.updated`, Redis `stock_price:{SYMBOL}`, STOMP `/topic/price/{SYMBOL}` |
| Called by | api-gateway (public routes), order-service, portfolio-service, account-service |

## Run and test

```powershell
.\mvnw.cmd spring-boot:run        # needs MySQL, Redis, Kafka (docker compose up -d)
.\mvnw.cmd test                   # contextLoads only; needs docker-compose infrastructure
```

## Package layout

```
com.stockmarket.marketdata
├── MarketDataServiceApplication        @EnableScheduling
├── config/DataInitializer              seeds 6 NASDAQ stocks when `stocks` is empty (CommandLineRunner)
├── config/RedisConfig                  RedisTemplate<String, StockPrice> with JacksonJsonRedisSerializer
├── config/WebSocketConfig              STOMP endpoint /ws (+ SockJS), simple broker /topic, app prefix /app
├── controller/MarketDataController     /api/v1/market-data
├── dto/StockPrice                      symbol, companyName, price, change, changePercent, high, low, open, volume, timestamp
├── entity/Stock, StockQuote            Stock: symbol PK, initialPrice, exchange, currency, timeZone, hours, active
├── repository/StockRepository          findByActiveTrue
└── service/MarketDataService           price cache, @Scheduled random walk, fan-out
```

## How prices work

1. `DataInitializer` (first start only): AAPL 195.30, MSFT 420.50, GOOG 175.20, AMZN 185.60, TSLA 250.10, NVDA 120.80.
2. On `ApplicationReadyEvent`, active stocks are loaded into a `ConcurrentHashMap<String, StockPrice>` at `initialPrice`. **This map is the source of truth**; entries are replaced, never mutated.
3. Every `market.price-update-interval` ms (60000) each price moves by a random ±0.25%; `change`/`changePercent` are relative to `open` (the start price); `high`/`low` track extremes; `volume` stays 0.
4. Each new price goes to Redis, STOMP and Kafka. One failing symbol does not stop the others.

Prices reset to `initialPrice` on every restart.

## API (`/api/v1/market-data`, public at the gateway)

| Method | Path | Returns |
|---|---|---|
| any | `/stocks` | `List<StockPrice>` |
| any | `/stocks/{symbol}` | `StockPrice` (symbol upper-cased) or 404 |
| GET | `/stocks/list` | plain text `SYMBOL - Company` lines |

WebSocket: connect to `ws://localhost:8082/ws`, subscribe to `/topic/price/AAPL`.

## Events

`stock.price.updated`, key symbol: `{symbol, price, timestamp}`. No consumers yet.

## Configuration

| Key | Notes |
|---|---|
| `server.port` | `MARKET_DATA_SERVICE_PORT` |
| `spring.datasource.url` | **hard-coded `localhost`** in the default profile (older pattern; no `hosts:` block) |
| `spring.data.redis.host/port` | localhost / `REDIS_PORT` |
| `spring.kafka.bootstrap-servers` | `localhost:${KAFKA_PORT}` |
| `market.price-update-interval` | ms between ticks |
| `docker` profile | switches to `MYSQL_HOST`, `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS` |

## Known limitations

- Only six simulated US stocks; Indian, crypto and forex symbols return 404 (account-service falls back to stored prices).
- `ALPHA_VANTAGE_API_KEY` / `FINNHUB_API_KEY` in `.env` are unused.
- `/stocks` and `/stocks/{symbol}` use `@RequestMapping` without a method, so they answer every HTTP method.
- `StockQuote` entity / `stock_quotes` table is unused; no price history.
- No consumer reads the Redis keys or `stock.price.updated`.
