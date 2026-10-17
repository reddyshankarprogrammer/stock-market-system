# Account Service: API Reference

Base URL through the gateway: `http://localhost:8080`. Every path below starts with `/api/v1`.
Interactive documentation: **Swagger UI** at `http://localhost:8086/swagger-ui.html` (service port; set `X-User-Id` yourself there).

- [Conventions](#conventions)
- [Account APIs](#account-apis)
- [Fund APIs](#fund-apis)
- [Trade APIs](#trade-apis)
- [Holding APIs](#holding-apis)
- [Statement APIs (import)](#statement-apis-import)
- [Statement and ledger APIs (generated)](#statement-and-ledger-apis-generated)
- [Charge and fee APIs](#charge-and-fee-apis)
- [Tax APIs](#tax-apis)
- [Performance API](#performance-api)
- [Dividend and interest APIs](#dividend-and-interest-apis)
- [Audit API](#audit-api)
- [Error responses](#error-responses)

## Conventions

| | |
|---|---|
| Authentication | `Authorization: Bearer <accessToken>` at the gateway; the gateway forwards `X-User-Id` and `X-User-Role` |
| Ownership | Every account-scoped call checks the account belongs to the caller. ADMIN may **read** any account; changes are owner-only |
| Format | JSON; amounts are decimals with 2 places; quantities up to 8 decimals; dates `yyyy-MM-dd`; date-times `yyyy-MM-ddTHH:mm:ss` |
| Ids | UUID strings |

Legend: 🔒 token required · 🛡️ ADMIN only.

## Account APIs

| Method | Path | | Purpose |
|---|---|---|---|
| POST | `/accounts` | 🔒 | Open an account (user validated with user-service) |
| GET | `/accounts/{id}` | 🔒 | Get an account |
| PUT | `/accounts/{id}` | 🔒 | Rename / change remarks |
| DELETE | `/accounts/{id}` | 🔒 | Close (soft delete) |
| GET | `/accounts/user/{userId}` | 🔒 | Accounts of a user (own, or any for ADMIN) |
| GET | `/accounts/{id}/summary` | 🔒 | Cash, holdings value, lifetime totals |

**POST /accounts**

| Field | Required | Rules |
|---|---|---|
| `accountName` | yes | not blank, ≤ 100 |
| `accountType` | yes | `INDIVIDUAL`, `JOINT`, `CORPORATE`, `EQUITY`, `DERIVATIVES`, `CRYPTO`, `FOREX` |
| `brokerName` | no | `PLATFORM` (default), `PAPER`, `GROWW`, `ZERODHA`, `BINANCE`, `EXNESS`, `MANUAL` |
| `currency` | no | 3-letter ISO code, default `USD` |
| `remarks` | no | ≤ 500 |

```json
{ "accountName": "Zerodha Equity", "accountType": "EQUITY", "brokerName": "ZERODHA", "remarks": "long term" }
```

`201 Created`, `Location: /api/v1/accounts/{id}`:

```json
{
  "id": "287efc03-3735-4e2a-aeff-e231d1d4093f",
  "accountNumber": "INV575743491179",
  "userId": "a8b3b431-e0d0-41dd-ab82-507c90470c46",
  "accountName": "Zerodha Equity",
  "accountType": "EQUITY",
  "brokerName": "ZERODHA",
  "currency": "USD",
  "remarks": "long term",
  "balance": 0.00,
  "status": "ACTIVE",
  "createdAt": "2026-10-17T14:05:10.123",
  "updatedAt": "2026-10-17T14:05:10.123",
  "closedAt": null
}
```

Errors: `400` validation or user not ACTIVE · `404` user unknown to user-service · `502` user-service down.

**DELETE /accounts/{id}**: allowed only when the balance is 0 and no units are held; returns the account with `status: CLOSED`.

**GET /accounts/{id}/summary** returns `cashBalance`, `openPositions`, `investedAmount`, `marketValue`, `unrealizedProfit`, `realizedProfit`, `totalEquity`, `totalDeposits`, `totalWithdrawals`, `totalFees`, `totalTaxes`, `totalDividends`, `totalInterest`, `pricesLive`.

## Fund APIs

| Method | Path | | Purpose |
|---|---|---|---|
| POST | `/accounts/{id}/deposit` | 🔒 | Add cash |
| POST | `/accounts/{id}/withdraw` | 🔒 | Remove cash; withdrawal fee charged on top |
| POST | `/accounts/{id}/adjustments` | 🔒 | Signed cash adjustment (statement reconciliation) |
| GET | `/accounts/{id}/fund-transactions` | 🔒 | All deposits, withdrawals and adjustments |

| Request | Fields and rules |
|---|---|
| Deposit / Withdraw | `amount` ≥ 0.01, 2 decimals; `reference` ≤ 100 (optional) |
| Adjustment | `amount` signed, ≠ 0, 2 decimals; `description` required ≤ 100; `statementId` optional |

```json
POST /api/v1/accounts/{id}/adjustments
{ "amount": -125.40, "description": "Match Zerodha ledger 30-Sep", "statementId": "d45f4f76-..." }
```

`201` → `FundTransactionResponse` (`transactionType` `DEPOSIT` / `WITHDRAWAL` / `ADJUSTMENT_IN` / `ADJUSTMENT_OUT`, `amount`, `feeAmount`, `balanceAfter`, `status`, `transactionDate`). `422` when the balance does not cover a withdrawal + fee or a negative adjustment.

## Trade APIs

| Method | Path | | Accounts | Purpose |
|---|---|---|---|---|
| POST | `/trades/buy` | 🔒 | PLATFORM, PAPER | Buy at the live price, settle in cash |
| POST | `/trades/sell` | 🔒 | PLATFORM, PAPER | Sell held units at the live price |
| POST | `/trades` | 🔒 | tracking | Record an external trade (never moves cash) |
| GET | `/trades/{id}` | 🔒 | all | One trade |
| GET | `/trades/account/{accountId}?from&to` | 🔒 | all | Trades, newest first; optional inclusive date range |

**POST /trades/buy | /trades/sell**: `{ "accountId": "...", "symbol": "AAPL", "quantity": 5 }` (`quantity` 1 to 1,000,000 whole shares).

**POST /trades** (manual trade):

| Field | Required | Rules |
|---|---|---|
| `accountId`, `symbol`, `side` (`BUY`/`SELL`), `quantity`, `tradeDate` | yes | quantity > 0, ≤ 8 decimals; tradeDate not in the future |
| `buyPrice` | for BUY | > 0 |
| `sellPrice` | for SELL | > 0 |
| both prices | | makes a **closed position**; `side` BUY = long, SELL = short |
| `brokerage`, `fee`, `tax` | no | ≥ 0; stored as charges/taxes linked to the trade |
| `exchange` ≤ 20, `externalTradeId` ≤ 100 (unique per account), `notes` ≤ 500 | no | |

```json
{
  "accountId": "e94e1cb0-...", "symbol": "BTCUSDT", "exchange": "BINANCE", "side": "BUY",
  "quantity": 0.5, "buyPrice": 60000, "brokerage": 12.50,
  "tradeDate": "2026-10-01T10:00:00", "externalTradeId": "7001"
}
```

`TradeResponse` (all trades):

```json
{
  "id": "4414c934-...", "accountId": "e94e1cb0-...", "userId": "a8b3b431-...",
  "symbol": "BTCUSDT", "exchange": "BINANCE", "side": "BUY",
  "tradeKind": "EXECUTION", "source": "MANUAL",
  "quantity": 0.5, "price": 60000.00000000, "buyPrice": 60000.00000000, "sellPrice": null,
  "grossAmount": 30000.00, "brokerage": 12.50, "otherFee": 0.00, "feeAmount": 12.50, "taxAmount": 0.00,
  "netAmount": 30012.50, "realizedProfit": null, "averageCostAtTrade": null,
  "balanceAfter": null, "tradeDate": "2026-10-01T10:00:00",
  "statementId": null, "externalTradeId": "7001", "notes": null
}
```

`balanceAfter` is set on platform BUY/SELL responses only. Errors: `400` wrong account kind / missing price / not enough units · `404` unknown symbol or account · `409` duplicate `externalTradeId` · `422` insufficient balance (platform) · `502` market data down.

## Holding APIs

| Method | Path | | Purpose |
|---|---|---|---|
| GET | `/holdings/account/{accountId}` | 🔒 | Open positions with live valuation |

```json
[{
  "id": "…", "accountId": "…", "userId": "…", "symbol": "INFY",
  "quantity": 15, "averagePrice": 1600.00000000, "totalCost": 24000.00, "investedAmount": 24000.00,
  "realizedProfit": 1000.00, "currentPrice": null, "marketValue": null,
  "unrealizedProfit": null, "unrealizedProfitPercent": null, "priceUpdatedAt": null
}]
```

Live fields come from market-data-service; when it has no price (for example Indian or crypto symbols) the last stored price is used, or the fields stay null.

## Statement APIs (import)

| Method | Path | | Purpose |
|---|---|---|---|
| POST | `/accounts/{accountId}/statements/upload` | 🔒 | Upload and import (multipart field `file`) |
| GET | `/accounts/{accountId}/statements` | 🔒 | Statements of an account, newest first |
| GET | `/statements/{statementId}` | 🔒 | One statement with its latest import |
| GET | `/statements/{statementId}/download` | 🔒 | Original file |
| GET | `/statements/{statementId}/imports` | 🔒 | Import history (every attempt) |
| POST | `/statements/{statementId}/reprocess` | 🔒 | Import again, replacing the previous import |
| DELETE | `/statements/{statementId}` | 🔒 | Delete file, statement and everything it imported (`204`) |

Upload rules: account is ACTIVE, owned by the caller, broker GROWW/ZERODHA/BINANCE/EXNESS; extension `xlsx`/`xls`/`csv` with matching content; size ≤ 20 MB; not the same file (SHA-256) as an earlier upload of the account.

```bash
curl -X POST http://localhost:8080/api/v1/accounts/$ACCOUNT/statements/upload \
  -H "Authorization: Bearer $TOKEN" -F "file=@tradebook.csv;type=text/csv"
```

`201 Created`:

```json
{
  "id": "d45f4f76-889e-4690-a652-11027add59f2",
  "accountId": "1685c344-…", "userId": "6c4f12b4-…", "brokerName": "ZERODHA",
  "originalFileName": "tradebook.csv", "fileFormat": "CSV", "fileSize": 294,
  "checksumSha256": "c92e178a…", "status": "PROCESSED",
  "periodFrom": "2026-10-01", "periodTo": "2026-10-05",
  "uploadedAt": "2026-10-17T14:01:56.78", "processedAt": "2026-10-17T14:01:57.02",
  "latestImport": {
    "attemptNumber": 1, "status": "SUCCESS", "parserName": "ZerodhaParser",
    "rowsRead": 6, "rowsSkipped": 0, "tradesImported": 3, "duplicateTrades": 0,
    "chargesImported": 2, "taxesImported": 1,
    "reportedRealizedPnl": null, "reportedNetPnl": null,
    "warnings": [], "errorMessage": null
  }
}
```

Statement `status`: `UPLOADED` → `PROCESSING` → `PROCESSED` / `PARTIALLY_PROCESSED` (skipped rows or warnings) / `FAILED`. Errors: `400` file or account rules · `409` same file already uploaded (`existingId` in the body) · `413` larger than 20 MB. See [statement import](account-service-statement-import.md).

## Statement and ledger APIs (generated)

| Method | Path | | Purpose |
|---|---|---|---|
| GET | `/accounts/{id}/statement?from=2026-10-01&to=2026-10-31` | 🔒 | Account statement (max 366 days) |
| GET | `/accounts/{id}/ledger?from&to` | 🔒 | Ledger entries with running balance |

The statement contains `openingBalance`, `totalCredits`, `totalDebits`, `closingBalance`, `totalFees`, `totalTaxes`, `ledgerEntries`, `trades`, `fundTransactions`, `dividends`, `interest`.

## Charge and fee APIs

| Method | Path | | Purpose |
|---|---|---|---|
| GET | `/charges/account/{accountId}` | 🔒 | Imported and manual charges |
| GET | `/charges/account/{accountId}/summary?year=2026` | 🔒 | Totals per charge type, including platform fees |
| GET | `/fees` | 🔒 | Platform fee schedule |
| POST | `/fees/calculate` | 🔒 | Fee preview `{ "amount": 1953.00, "kind": "TRADE" \| "WITHDRAWAL" }` |
| GET | `/fees/account/{accountId}` | 🔒 | Platform fees charged |

Charge types: `BROKERAGE`, `EXCHANGE_FEE`, `STT`, `GST`, `DP_CHARGE`, `NETWORK_FEE`, `OTHER`. Platform fees are mapped as BROKERAGE → BROKERAGE, EXCHANGE → EXCHANGE_FEE, PLATFORM/WITHDRAWAL → OTHER.

```json
GET /api/v1/charges/account/{id}/summary?year=2026
{ "accountId": "…", "year": 2026,
  "byType": [ { "type": "BROKERAGE", "amount": 20.00 }, { "type": "STT", "amount": 15.50 } ],
  "totalCharges": 35.50 }
```

`year` must be 2000–2100; default is the current year.

## Tax APIs

| Method | Path | | Purpose |
|---|---|---|---|
| GET | `/taxes` | 🔒 | Active tax rules |
| POST | `/taxes/calculate` | 🔒 | Tax preview `{ "side": "SELL", "tradeValue": 1000, "realizedProfit": 120.50 }` |
| GET | `/taxes/account/{accountId}` | 🔒 | Taxes charged (engine, manual, imported) |
| GET | `/taxes/account/{accountId}/summary?year=2026` | 🔒 | Yearly totals per type and per month |

Tax types: `TRADE_TAX`, `CAPITAL_GAINS_TAX`, `DIVIDEND_TAX`, `GST`, `VAT`, `TDS`.

```json
{ "accountId": "…", "year": 2026,
  "byType": [ { "type": "GST", "amount": 3.60 } ],
  "byMonth": [ { "month": 10, "amount": 3.60 } ],
  "totalTaxes": 3.60 }
```

## Performance API

| Method | Path | | Purpose |
|---|---|---|---|
| GET | `/accounts/{id}/performance` | 🔒 | Recalculate and return performance |

```json
{
  "accountId": "…", "userId": "…", "brokerName": "ZERODHA",
  "grossProfit": 1000.00, "realizedProfit": 1000.00, "unrealizedProfit": 0.00,
  "totalCharges": 35.50, "totalTaxes": 3.60, "netProfit": 960.90,
  "pricesLive": false, "calculatedAt": "2026-10-17T14:02:01.5"
}
```

`grossProfit = realizedProfit + unrealizedProfit + fees already deducted inside realizedProfit`; `netProfit = grossProfit − totalCharges − totalTaxes`. The result is also stored in `account_performance`.

## Dividend and interest APIs

| Method | Path | | Purpose |
|---|---|---|---|
| POST | `/accounts/{id}/dividends` | 🛡️ | Pay `dividendPerShare` on units held, minus dividend tax |
| GET | `/dividends/account/{accountId}` | 🔒 | Dividends received |
| POST | `/accounts/{id}/interest` | 🛡️ | Simple interest on the balance for a period |
| GET | `/interest/account/{accountId}` | 🔒 | Interest received |

```json
POST /accounts/{id}/dividends   { "symbol": "INFY", "dividendPerShare": 10, "paymentDate": "2026-10-10" }
POST /accounts/{id}/interest    { "annualRate": 4.5, "periodStart": "2026-09-01", "periodEnd": "2026-09-30" }
```

Non-admin tokens are rejected by the gateway (403) and again by the service.

## Audit API

| Method | Path | | Purpose |
|---|---|---|---|
| GET | `/accounts/{id}/audit` | 🔒 | All actions on the account, newest first |

```json
[{
  "id": "…", "entityType": "InvestmentAccount", "entityId": "…",
  "action": "STATEMENT_PROCESS", "performedBy": "6c4f12b4-…",
  "details": "Imported statement d45f…: 3 trades, 2 charges, 1 taxes (SUCCESS)",
  "requestJson": null, "responseJson": "{\"attemptNumber\":1,\"status\":\"SUCCESS\",…}",
  "createdAt": "2026-10-17T14:01:57.01"
}]
```

Actions: `CREATE`, `UPDATE`, `CLOSE`, `DEPOSIT`, `WITHDRAW`, `ADJUSTMENT`, `BUY`, `SELL`, `TRADE_CREATE`, `HOLDING_UPDATE`, `FEE_CALCULATION`, `TAX_CALCULATION`, `DIVIDEND`, `INTEREST`, `STATEMENT_UPLOAD`, `STATEMENT_PROCESS`, `STATEMENT_DELETE`.

## Error responses

```json
{ "timestamp": "2026-10-17T14:01:57.29", "status": 409, "error": "Conflict",
  "message": "This file was already uploaded as statement d45f4f76-…", "existingId": "d45f4f76-…" }
```

| Status | When |
|---|---|
| 400 | Field validation (`errors` map per field), business rule (closed account, wrong account kind, not enough units, bad file), malformed request |
| 401 | No `X-User-Id` (service) / no or invalid token (gateway) |
| 403 | Another user's account; ADMIN-only endpoint |
| 404 | Unknown account, trade, statement, user, stock or endpoint |
| 405 | Wrong HTTP method |
| 409 | Duplicate statement file or `externalTradeId`; concurrent update of the same account (retry) |
| 413 | Upload larger than 20 MB |
| 422 | Balance does not cover the debit (`required`, `available` in the body) |
| 502 | market-data-service or user-service unavailable |
