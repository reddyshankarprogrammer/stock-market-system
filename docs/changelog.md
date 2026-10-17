# Changelog

All notable changes to the Stock Market System. Dates are when the change was made.

## 2026-10-17: Postman collection

- 17 more requests (102 in total): account rename and close, fund transactions, fees and taxes charged, tax rules, dividend and interest lists, and an upload of every broker sample (Groww, Binance, Exness).
- Every account-service endpoint is now covered.

## 2026-10-17: account-service v2.0.0, roles

### Features

| Feature | Services |
|---|---|
| Multiple investment accounts across brokers | account-service |
| User validation before account creation (OpenFeign) | account-service |
| USER / ADMIN roles end to end | user-service, api-gateway, account-service |
| Statement upload, download, history, reprocess, delete | account-service |
| Statement import engine (Groww, Zerodha, Binance, Exness; configurable columns) | account-service |
| Manual trades, closed positions, fractional quantities | account-service |
| Holdings rebuild for tracking accounts; valuation snapshot | account-service |
| Imported/manual charges and taxes; yearly summaries | account-service |
| Account performance | account-service |
| Audit request/response JSON | account-service |
| Cash adjustments | account-service |

### Files changed

| Service | Files |
|---|---|
| user-service | `entity/Role.java` (new), `entity/User.java`, `security/JwtService.java`, `dto/UserResponse.java`, `service/impl/UserServiceImpl.java`, `controller/UserController.java`, tests |
| api-gateway | `filter/JwtAuthFilter.java` (role forwarding, ADMIN paths), `application.yaml` (account-service routes), `JwtAuthFilterTest.java` |
| account-service | `pom.xml` (OpenFeign, POI, Commons CSV); `enums/` (BrokerName, RecordSource, TradeKind, ChargeType, StatementFormat, StatementStatus, ImportStatus, TradeMode + extended enums); `entity/` (AccountStatement, StatementImportHistory, AccountCharge, AccountPerformance + extended Trade, Holding, InvestmentAccount, AccountTax, Dividend, AuditLog); `repository/` (4 new + extended); `dto/` (ManualTradeRequest, AdjustmentRequest, AccountStatementResponse, StatementImportHistoryResponse, AccountChargeResponse, ChargeSummaryResponse, TaxSummaryResponse, PerformanceResponse, TypeAmountResponse, MonthAmountResponse + extended); `client/UserServiceClient`, `UserDto`; `config/FeignConfig`, `StatementConfig`; `security/RequestUser`; `service/` (ChargeService, PerformanceService, UserValidationService, TrackedChargeRecorder + extended Trade/Holding/Fund/Account/Tax/Audit services); `statement/` module (controller, service, parser, storage); `controller/ChargeController` + extended controllers; `mapper/AccountMapper`; `application.yaml`; tests |
| scripts | `start-all.ps1` (Docker stderr no longer aborts the script) |
| docs | 8 account-service documents, this changelog, README, getting-started, user manual, technical documentation, Postman collection |

### Database scripts added

`V3__investment_account_broker.sql`, `V4__account_statement.sql`, `V5__statement_import_history.sql`, `V6__trade.sql`, `V7__holding.sql`, `V8__account_charge.sql`, `V9__account_tax.sql`, `V10__account_performance.sql`, `V11__audit_log.sql`. user-service adds `users.role` through Hibernate.

### New APIs

See [release notes](account-service-release-notes.md#new-apis).

### Updated APIs

| API | Change |
|---|---|
| `POST /api/v1/accounts` | `brokerName`, `remarks`; validates the user with user-service |
| `PUT /api/v1/accounts/{id}` | `remarks` |
| `GET /api/v1/accounts/user/{userId}` | ADMIN may list any existing user |
| Account-scoped GETs | ADMIN may read any account |
| `POST /api/v1/trades/buy`, `/sell` | Only PLATFORM / PAPER accounts; response has new fields |
| `POST /api/v1/accounts/{id}/dividends`, `/interest` | ADMIN role required |
| `GET /api/v1/users/{userId}` (user-service) | ADMIN may read any user; responses include `role` |
| All JWTs | New `role` claim; gateway forwards `X-User-Role` |

### Deprecations

None.

## 2026-10-16: account-service v1.0.0 and platform fixes

- account-service v1: accounts, ledger, platform trades, fees, taxes, holdings, dividends, interest, statements, summary, audit (V1, V2).
- api-gateway: JWT filter rewritten for gateway server-webmvc; internal funds endpoints blocked.
- user-service: loads `.env`; ownership checks on profile and add-funds.
- order-service, notification-service, portfolio-service: fixes; Spring Cloud 2025.1.3.
- Documentation, Postman collection and start/stop scripts added.
