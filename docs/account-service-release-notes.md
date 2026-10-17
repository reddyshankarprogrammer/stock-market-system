# Account Service: Release Notes

## v2.0.0 (2026-10-17)

### New features

- **Multi investment accounts**: one user, many accounts across brokers (`brokerName`: PLATFORM, PAPER, GROWW, ZERODHA, BINANCE, EXNESS, MANUAL), with remarks and new account types (EQUITY, DERIVATIVES, CRYPTO, FOREX).
- **User service integration**: accounts are only opened for users that exist and are ACTIVE in user-service (OpenFeign, identity forwarded).
- **Roles**: USER and ADMIN end to end: role in the user, in the JWT, forwarded by the gateway as `X-User-Role`. ADMIN can read any account and post dividends/interest.
- **Statement upload**: xlsx, xls, csv up to 20 MB; stored on disk; download, history, reprocess, delete.
- **Statement processing**: configurable parsers for Groww, Zerodha, Binance and Exness (trades, charges, taxes, P&L summary) with per-row error reporting and idempotent reprocessing.
- **Trade management**: manual trades for tracking accounts (`POST /trades`), get by id, fractional quantities, closed positions (long and short), exchange, brokerage/fee/tax, external trade id.
- **Holdings**: fractional units, invested amount, stored valuation snapshot; tracking accounts rebuild holdings from their full trade history.
- **Charges**: `account_charges` for imported/manual charges; yearly charge summary including platform fees.
- **Taxes**: imported/manual taxes; GST, VAT, TDS types; yearly tax summary by type and month.
- **Performance tracking**: `GET /accounts/{id}/performance` (`netProfit = grossProfit − totalCharges − totalTaxes`), stored per account.
- **Audit logging**: every action keeps request and response JSON; new actions for statements, manual trades, holding updates and fee/tax calculations.
- **Cash adjustments**: `POST /accounts/{id}/adjustments` (STATEMENT_ADJUSTMENT ledger category).

### New APIs

`POST /trades` · `GET /trades/{id}` · `POST /accounts/{id}/adjustments` · `GET /accounts/{id}/performance` · `POST /accounts/{accountId}/statements/upload` · `GET /accounts/{accountId}/statements` · `GET /statements/{id}` · `GET /statements/{id}/download` · `GET /statements/{id}/imports` · `POST /statements/{id}/reprocess` · `DELETE /statements/{id}` · `GET /charges/account/{accountId}` · `GET /charges/account/{accountId}/summary` · `GET /taxes/account/{accountId}/summary` (all under `/api/v1`).

### Migration notes

1. Deploy **user-service** first: it adds `users.role` (Hibernate update, default `USER` for existing rows) and puts `role` in new tokens.
2. Deploy **api-gateway**: forwards `X-User-Role`; new routes `/api/v1/statements/**` and `/api/v1/charges/**`; ADMIN-only dividend/interest paths.
3. Deploy **account-service**: Flyway applies V3 to V11 on start (about 5 s on the development data). Existing trades and holdings are backfilled (`user_id`, buy/sell price, brokerage split).
4. Promote admins: `UPDATE users SET role='ADMIN' WHERE email='…';` then log in again.
5. Create the statements directory or volume (`ACCOUNT_STATEMENTS_DIR`, default `uploads/statements`).
6. Review the broker column defaults in `application.yaml` against real statement files.

### Database changes

| Script | Change |
|---|---|
| V3__investment_account_broker | `broker_name` (default PLATFORM), `remarks`; indexes on broker_name and (user_id, broker_name) |
| V4__account_statement | new `account_statements` |
| V5__statement_import_history | new `statement_import_history` |
| V6__trade | `account_trades`: decimal quantity/prices, user_id, exchange, trade_kind, source, buy/sell price, brokerage, other_fee, statement_id, external_trade_id, notes |
| V7__holding | `account_holdings`: decimal quantity/average, user_id, current_price, market_value, unrealized_pnl, price_updated_at; `account_dividends.quantity` decimal |
| V8__account_charge | new `account_charges` |
| V9__account_tax | `account_taxes`: tax_master_id nullable, source, statement_id, description |
| V10__account_performance | new `account_performance` |
| V11__audit_log | `account_audit_logs`: request_json, response_json |

Existing scripts V1 and V2 are unchanged.

### Breaking changes

None for existing clients of v1 endpoints. Behavioural changes to be aware of:

- `POST /trades/buy` and `/trades/sell` now return `400` for tracking accounts (only PLATFORM/PAPER trade at market price). All v1 accounts are PLATFORM.
- `POST /accounts` now calls user-service; it returns `502` if user-service is down and `400` for non-ACTIVE users.
- Dividend and interest posting require the ADMIN role (previously blocked at the gateway for everyone).
- Account reads by an ADMIN of another user's account now succeed (previously 403).
- The audit trail has more entries per trade (BUY/SELL plus FEE_CALCULATION, TAX_CALCULATION, HOLDING_UPDATE).

### Compatibility notes

- Response fields were only **added**. `quantity` stays a JSON number; whole quantities are still written without decimals (`5`), fractional ones as decimals (`0.5`). Prices may now carry up to 8 decimals.
- The `TradeExecutedEvent` Kafka payload is unchanged (platform trades only, integer quantity).
- Tokens issued before v2 (no `role` claim) keep working as USER until they expire.
- Spring Boot 4.0.8 / Spring Cloud 2025.1.3, same as the other services (not Boot 3.5).
- API paths keep the `/api/v1` prefix used across the system.

## v1.0.0 (2026-10-16)

First release: investment accounts, deposits and withdrawals with fees, running-balance ledger, platform BUY/SELL at market price with fee and tax engines, holdings, dividends and interest, statements, account summary, audit trail, Kafka events, Swagger, Testcontainers tests.
