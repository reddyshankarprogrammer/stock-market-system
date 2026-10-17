# account-service

Investment accounts across brokers: cash ledger, platform and tracked trades, broker statement import, holdings, fees, charges, taxes, dividends, interest, performance and audit.

| | |
|---|---|
| Port | `ACCOUNT_SERVICE_PORT` (8086) |
| Base package | `com.stockmarket.accountservice` |
| Stack | Boot 4.0.8, Spring Cloud 2025.1.3 (OpenFeign), Flyway 11, MapStruct 1.6, Apache POI 5.4, Commons CSV 1.14, springdoc 3.0 |
| Database | MySQL, **Flyway V1–V11** (`account_flyway_schema_history`), Hibernate `ddl-auto: validate` |
| Calls | user-service (Feign: `/me`, `/{id}`), market-data-service (RestClient: prices) |
| Publishes | Kafka `account.trade.executed`, `account.fund.transaction` (after commit) |
| Swagger | <http://localhost:8086/swagger-ui.html> |
| Version | 2.0.0 ([release notes](../docs/account-service-release-notes.md)) |

Detailed documentation (read these for anything non-trivial):

| Topic | Document |
|---|---|
| Purpose, architecture, integrations | [docs/account-service-overview.md](../docs/account-service-overview.md) |
| Every endpoint, fields, errors | [docs/account-service-api.md](../docs/account-service-api.md) |
| Tables, ER diagram, migrations | [docs/account-service-database.md](../docs/account-service-database.md) |
| Flows | [docs/account-service-sequence-diagrams.md](../docs/account-service-sequence-diagrams.md) |
| Roles, ownership, Feign | [docs/account-service-security.md](../docs/account-service-security.md) |
| Statement import, parsers | [docs/account-service-statement-import.md](../docs/account-service-statement-import.md) |
| Ledger postings | [docs/account-service-ledger-design.md](../docs/account-service-ledger-design.md) |

## Run and test

```powershell
.\mvnw.cmd spring-boot:run        # needs MySQL + Kafka; user-service and market-data-service for full behaviour
.\mvnw.cmd test                   # 44 tests (~1 min); Testcontainers starts its own MySQL 8.0 + Kafka (Docker required)
docker compose --profile app up -d account-service   # optional: run it in Docker (from the repo root)
```

## Package layout

```
com.stockmarket.accountservice
├── client/            MarketDataClient (RestClient, timeouts), UserServiceClient (Feign) + UserDto
├── config/            AuditConfig (JPA auditing), FeignConfig (identity propagation), JacksonConfig,
│                      SwaggerConfig, StatementConfig (binds statement + import properties)
├── controller/        Account, Fund, Trade, Holding, Fee, Charge, Tax, Dividend, Interest, Statement (generated)
│                      + ApiHeaders
├── dto/request|response   generated from one script; Bean Validation on requests
├── entity/            BaseEntity (UUID id, @Version, audit columns) + 15 entities
├── enums/             BrokerName, AccountType, TradeSide, TradeKind, RecordSource, LedgerCategory, ...
├── event/             TradeExecutedEvent, FundTransactionEvent, AccountEventPublisher (AFTER_COMMIT -> Kafka)
├── exception/         ResourceNotFound, AccountAccessDenied, InsufficientBalance, Validation, DuplicateResource,
│                      MarketDataUnavailable, UserServiceUnavailable + GlobalExceptionHandler
├── mapper/AccountMapper   MapStruct, unmappedTargetPolicy = ERROR
├── repository/        derived queries + JPQL (locks, sums, per-type/month aggregates)
├── security/RequestUser   X-User-Id / X-User-Role of the current request
├── service/           interfaces
│   ├── impl/          implementations + TrackedChargeRecorder
│   └── model/         Money (rounding), FeeBreakdown/FeeCharge, TaxBreakdown/TaxCharge, LedgerStatement
└── statement/         upload/import module
    ├── controller/AccountStatementController
    ├── service/       AccountStatementService (upload, list, download, reprocess, delete), StatementImportService
    ├── parser/        StatementParser, AbstractConfigurableParser, Groww/Zerodha/Binance/ExnessParser,
    │                  ParserFactory, TabularDataReader (CSV/XLS/XLSX), CellValues, BrokerImportProperties
    └── storage/       StatementStorageService (local disk), StatementStorageProperties
```

Resources: `application.yaml` (incl. `account.import.brokers.*` statement layouts), `db/migration/V1..V11`.

## Core concepts

| Concept | Meaning |
|---|---|
| Platform-traded account | `brokerName` PLATFORM or PAPER. `POST /trades/buy|sell` at the market-data price; cash moves |
| Tracking account | GROWW, ZERODHA, BINANCE, EXNESS, MANUAL. Trades via `POST /trades` or statement upload; **never move cash** |
| Ledger | Only `LedgerService` changes a balance; one entry per change with `entry_number` and `balance_after` |
| Trade kind | EXECUTION (changes holdings) or CLOSED_POSITION (buy+sell price, realized P&L only) |
| Record source | PLATFORM, MANUAL, IMPORT (trades, charges, taxes) |
| Fees vs charges | `account_fees` = platform fee engine; `account_charges` = imported/manual |
| Holdings | PLATFORM: updated incrementally. Tracking: rebuilt from the full trade history (`rebuildHoldings`) |
| Performance | `netProfit = grossProfit − totalCharges − totalTaxes` |
| Roles | ADMIN reads any account and posts dividends/interest; changes are owner-only |

## API summary (`/api/v1`)

| Area | Endpoints |
|---|---|
| Accounts | `POST /accounts`, `GET|PUT|DELETE /accounts/{id}`, `GET /accounts/user/{userId}`, `GET /accounts/{id}/summary`, `/performance`, `/audit` |
| Funds | `POST /accounts/{id}/deposit`, `/withdraw`, `/adjustments`, `GET /accounts/{id}/fund-transactions` |
| Trades | `POST /trades/buy`, `/trades/sell`, `POST /trades`, `GET /trades/{id}`, `GET /trades/account/{accountId}` |
| Holdings | `GET /holdings/account/{accountId}` |
| Statements (import) | `POST /accounts/{id}/statements/upload`, `GET /accounts/{id}/statements`, `GET /statements/{id}`, `/download`, `/imports`, `POST /statements/{id}/reprocess`, `DELETE /statements/{id}` |
| Statements (generated) | `GET /accounts/{id}/statement?from&to`, `GET /accounts/{id}/ledger?from&to` |
| Fees / charges / taxes | `GET /fees`, `POST /fees/calculate`, `GET /fees/account/{id}`, `GET /charges/account/{id}[/summary]`, `GET /taxes`, `POST /taxes/calculate`, `GET /taxes/account/{id}[/summary]` |
| Income (ADMIN) | `POST /accounts/{id}/dividends`, `POST /accounts/{id}/interest`; reads `GET /dividends/account/{id}`, `GET /interest/account/{id}` |

## Configuration

| Key | Default / notes |
|---|---|
| `hosts.mysql`, `hosts.kafka`, `hosts.market-data-service`, `hosts.user-service` | localhost; `docker` profile → container names |
| `services.market-data.url` (+ `connect-timeout` 2s, `read-timeout` 3s) | market data |
| `services.user.url`; `spring.cloud.openfeign.client.config.user-service.*` | user-service Feign, 2s / 3s |
| `spring.flyway.table`, `baseline-on-migrate`, `baseline-version: 0` | shared schema handling |
| `spring.servlet.multipart.max-file-size` 20MB, `account.statements.max-file-size` 20MB | upload limit |
| `account.statements.storage-dir` | `ACCOUNT_STATEMENTS_DIR` or `uploads/statements` (git-ignored) |
| `account.import.max-rows`, `header-search-rows`, `brokers.<broker>.*` | statement layouts |
| `kafka.topics.trade-executed`, `kafka.topics.fund-transaction` | published topics |

## Tests

| Class | Kind |
|---|---|
| `AccountServiceApplicationTests` | Testcontainers: v1 lifecycle, rules, ownership, ADMIN, performance, previews |
| `AccountServiceV2Tests` | Testcontainers: Zerodha/Exness imports, reprocess, delete, manual trades, adjustments, user validation |
| `FeeCalculationServiceImplTest`, `TaxCalculationServiceImplTest`, `LedgerServiceImplTest`, `HoldingServiceImplTest` | unit |
| `StatementParsersTest` | parsers against the real `application.yaml` layouts |

Both integration classes use `@SpringBootTest(properties = "account.statements.storage-dir=target/test-uploads")` so they share one Spring context and one set of containers.

## Known limitations

- Broker column defaults follow public export formats and are not yet confirmed with real statement files.
- Statements are processed synchronously in the upload request.
- Prices exist only for market-data's six US stocks; other symbols keep their last stored price.
- Separate ledger: platform orders from order-service are not reflected here (and vice versa).
