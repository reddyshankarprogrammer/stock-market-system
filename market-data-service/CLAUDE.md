# CLAUDE.md: market-data-service

Read `README.md` in this folder first.

## Rules

- Base package is `com.stockmarket.marketdata`; keep new classes under it.
- The in-memory `stockPriceCache` in `MarketDataService` is the source of truth. Always **replace** entries with a new `StockPrice`; never mutate one in place (readers on other threads must see consistent snapshots).
- Every price change must go through `publish(...)` so Redis, STOMP and Kafka stay in sync.
- The REST response shape (`StockPrice`) is a contract: order-service (`StockPriceResponse`), portfolio-service (`Map`) and account-service (`MarketDataClient.StockPrice`) read `symbol` and `price`. Add fields freely; do not rename or remove `price`.
- Boot 4 / Jackson 3: use `JacksonJsonSerializer` (Kafka) and `JacksonJsonRedisSerializer` (Redis).

## Gotchas

- Config uses hard-coded `localhost` rather than the `hosts:` block the other services use; if you touch hosts, migrate it to that pattern (see user-service `application.yaml`).
- `DataInitializer` only seeds an **empty** table; to add a stock to an existing database, insert a row (it is loaded on the next start) or add an admin endpoint.
- The `contextLoads` test needs MySQL, Redis and Kafka from docker compose.
- The scheduler interval is long (60 s): when testing price movement, lower `market.price-update-interval` locally rather than changing the default.

## Recipes

**Add a stock**: add it to `DataInitializer` (new databases) and insert into `stocks` (existing ones); restart.

**Use a real price feed**: add a provider interface (e.g. `PriceProvider`) with a simulator and an Alpha Vantage/Finnhub implementation; call it from `updatePrices()` and keep the cache/publish flow unchanged. Keys come from `.env` (`ALPHA_VANTAGE_API_KEY`, `FINNHUB_API_KEY`).

**Add an endpoint**: put it in `MarketDataController` with an explicit `@GetMapping`; it is public at the gateway (`/api/v1/market-data/**` is in `PUBLIC_PATHS`), so never expose anything user-specific here.

## Verify

```powershell
.\mvnw.cmd test
```
`GET http://localhost:8080/api/v1/market-data/stocks/AAPL` (Postman folder 2).
