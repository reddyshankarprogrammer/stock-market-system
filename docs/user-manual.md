# User Manual

How to use every service of the Stock Market System through its REST API: signing up, checking prices, trading, reading your portfolio and notifications, and managing investment accounts across brokers with account-service.

This manual assumes the system is running. If it is not, follow [Getting started](getting-started.md) first.

- [1. How the API works](#1-how-the-api-works)
- [2. A complete trading session, step by step](#2-a-complete-trading-session-step-by-step)
- [3. API reference](#3-api-reference)
  - [3.1 Users and wallet (user-service)](#31-users-and-wallet-user-service)
  - [3.2 Market data (market-data-service)](#32-market-data-market-data-service)
  - [3.3 Orders (order-service)](#33-orders-order-service)
  - [3.4 Portfolio (portfolio-service)](#34-portfolio-portfolio-service)
  - [3.5 Notifications (notification-service)](#35-notifications-notification-service)
  - [3.6 Live prices over WebSocket](#36-live-prices-over-websocket)
  - [3.7 Health checks](#37-health-checks)
  - [3.8 Investment accounts (account-service)](#38-investment-accounts-account-service)
- [4. Errors](#4-errors)
- [5. Rules and limits](#5-rules-and-limits)
- [6. Testing with Postman](#6-testing-with-postman)

---

## 1. How the API works

**One address.** Send every request to the API gateway at `http://localhost:8080`. The gateway forwards it to the right service. The individual service ports (8081 to 8085) are for debugging only.

**Log in once, send the token every time.** Registering or logging in returns an `accessToken`. Send it on every other request:

```
Authorization: Bearer <accessToken>
```

The token identifies you, so you never send your own user id in a header. The gateway reads it from the token and passes it to the services. An access token is valid for **15 minutes**; after that, log in again.

**Public endpoints** (no token needed): register, login, and all market-data endpoints.

**Format.** Request and response bodies are JSON (`Content-Type: application/json`). Amounts are decimals with two places. Ids are UUID strings.

## 2. A complete trading session, step by step

The examples use `curl`; the same requests are in the Postman collection (see [section 6](#6-testing-with-postman)).

### Step 1: Create an account

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"asha@example.com","password":"Secret123","firstName":"Asha","lastName":"Rao"}'
```

```json
{
  "userId": "8b58b586-bf72-421a-aac8-03b8cfcb97ff",
  "email": "asha@example.com",
  "firstName": "Asha",
  "lastName": "Rao",
  "walletBalance": 10000,
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

Your wallet starts with **10,000**. Copy the `accessToken`; the next steps call it `$TOKEN`. You also receive a welcome notification.

### Step 2: Look at the market

```bash
curl http://localhost:8080/api/v1/market-data/stocks/AAPL
```

```json
{
  "symbol": "AAPL",
  "companyName": "Apple Inc.",
  "price": 194.54,
  "change": -0.76,
  "changePercent": -0.39,
  "high": 195.30,
  "low": 194.54,
  "open": 195.30,
  "volume": 0,
  "timestamp": "2026-10-16T21:18:18.84"
}
```

Prices move a little every minute.

### Step 3: Buy shares

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"symbol":"AAPL","type":"BUY","quantity":5}'
```

```json
{
  "id": "4414c934-b988-4ad3-bb12-7fe345fc95f1",
  "userId": "8b58b586-...",
  "symbol": "AAPL",
  "type": "BUY",
  "status": "EXECUTED",
  "quantity": 5,
  "price": 195.30,
  "totalAmount": 976.50,
  "failureReason": null,
  "createdAt": "2026-10-16T21:10:09.91",
  "executedAt": "2026-10-16T21:10:09.98"
}
```

The order fills immediately at the current market price, and `totalAmount` (price × quantity) is taken from your wallet.

### Step 4: Check your portfolio

```bash
curl http://localhost:8080/api/v1/portfolio/me -H "Authorization: Bearer $TOKEN"
```

```json
{
  "userId": "8b58b586-...",
  "totalHoldings": 1,
  "totalInvested": 976.50,
  "currentPortfolioValue": 976.50,
  "totalProfitLoss": 0.00,
  "portfolioReturnPercentage": 0.00,
  "holdings": [
    {
      "symbol": "AAPL",
      "companyName": "AAPL",
      "quantity": 5,
      "averageBuyPrice": 195.30,
      "currentPrice": 195.30,
      "investedAmount": 976.50,
      "currentValue": 976.50,
      "profitLoss": 0.00,
      "profitLossPercentage": 0.00
    }
  ]
}
```

The portfolio updates about a second after the order. Profit/loss uses the live price, so it changes as the market moves.

### Step 5: Sell some shares

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"symbol":"AAPL","type":"SELL","quantity":2}'
```

The sale amount is added to your wallet, and the AAPL holding drops to 3 shares.

### Step 6: Review orders, wallet and notifications

```bash
curl http://localhost:8080/api/v1/orders/my-orders -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/api/v1/users/me         -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/api/v1/notifications    -H "Authorization: Bearer $TOKEN"
```

Notifications list the welcome message and one message per order, newest first.

## 3. API reference

All paths are relative to `http://localhost:8080`. 🔒 means a token is required.

### 3.1 Users and wallet (user-service)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/users/register` | public | Create an account and log in |
| POST | `/api/v1/users/login` | public | Log in, get new tokens |
| GET | `/api/v1/users/me` | 🔒 | Your profile and wallet balance |
| GET | `/api/v1/users/{userId}` | 🔒 | A profile by id (only your own) |
| POST | `/api/v1/users/{userId}/funds/add?amount=` | 🔒 | Add money to your wallet (only your own) |

**Register** `POST /api/v1/users/register`

| Field | Required | Rules |
|---|---|---|
| `email` | yes | valid e-mail, not already registered |
| `password` | yes | at least 6 characters |
| `firstName` | yes | not blank |
| `lastName` | yes | not blank |
| `initialBalance` | no | starting wallet amount, default `10000` |
| `mobileCountryCode` | no | `+` and 1–4 digits, e.g. `+91`; send together with `mobileNumber` |
| `mobileNumber` | no | 6–14 digits; stored as entered, not verified |
| `gender` | no | `MALE` or `FEMALE` |
| `country` | no | 2-letter uppercase ISO code, e.g. `IN` |
| `termsVersion` | no | version of the Terms and Risk Disclaimer accepted; the server records the time |

Returns `201 Created` with the body shown in [step 1](#step-1-create-an-account).

**Login** `POST /api/v1/users/login`

```json
{ "email": "asha@example.com", "password": "Secret123" }
```

Returns `200 OK` with the same body as register.

**My profile** `GET /api/v1/users/me` 🔒

```json
{
  "id": "8b58b586-...",
  "email": "asha@example.com",
  "firstName": "Asha",
  "lastName": "Rao",
  "walletBalance": 9023.50,
  "status": "ACTIVE",
  "createdAt": "2026-10-16T21:10:07.29"
}
```

**Add funds** `POST /api/v1/users/{userId}/funds/add?amount=5000` 🔒

`{userId}` must be your own id, otherwise `403`. `amount` must be greater than zero. Returns your updated profile.

> `/funds/deduct` and `/funds/credit` also exist, but only order-service may call them. The gateway blocks them with `403`.

### 3.2 Market data (market-data-service)

All public, no token needed.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/market-data/stocks` | Current price of every stock |
| GET | `/api/v1/market-data/stocks/{symbol}` | Current price of one stock (symbol is case-insensitive) |
| GET | `/api/v1/market-data/stocks/list` | Plain-text list of symbols and company names |

Available symbols: **AAPL, MSFT, GOOG, AMZN, TSLA, NVDA**.

Price fields:

| Field | Meaning |
|---|---|
| `price` | Current price |
| `open` | Price when the service started (the simulated "open") |
| `change`, `changePercent` | Difference from `open` |
| `high`, `low` | Highest and lowest price since the service started |
| `volume` | Always 0 in the simulation |
| `timestamp` | When this price was produced |

An unknown symbol returns `404`.

### 3.3 Orders (order-service)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/orders` | 🔒 | Place a BUY or SELL market order |
| GET | `/api/v1/orders/my-orders` | 🔒 | All your orders, newest first |
| GET | `/api/v1/orders/{orderId}` | 🔒 | One of your orders |

**Place an order** `POST /api/v1/orders` 🔒

| Field | Required | Rules |
|---|---|---|
| `symbol` | yes | one of the available symbols |
| `type` | yes | `BUY` or `SELL` |
| `quantity` | yes | whole number, at least 1 |
| `price` | no | ignored; all orders are market orders at the current price |

What happens:

1. The current price is fetched from market-data-service.
2. The order is saved as `PENDING` with `totalAmount = price × quantity`.
3. **BUY**: `totalAmount` is deducted from your wallet. **SELL**: it is credited.
4. On success the order becomes `EXECUTED` (`201 Created`). If the wallet step fails, the order is saved as `FAILED` with a `failureReason`, and you receive `422` with the order id.

Either way, a notification is created.

**Order statuses**

| Status | Meaning |
|---|---|
| `PENDING` | Saved, funds not yet settled (only seen briefly) |
| `EXECUTED` | Filled; wallet updated |
| `FAILED` | Not filled; see `failureReason` (for example *Insufficient wallet balance*) |

**Get an order** returns `404` if it does not exist and `403` if it belongs to someone else.

### 3.4 Portfolio (portfolio-service)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/v1/portfolio/me` | 🔒 | Your holdings with live profit/loss |

See [step 4](#step-4-check-your-portfolio) for the response.

| Field | Meaning |
|---|---|
| `quantity` | Shares you hold |
| `averageBuyPrice` | Weighted average price paid |
| `investedAmount` | Total paid for the shares you still hold |
| `currentValue` | `currentPrice × quantity` |
| `profitLoss`, `profitLossPercentage` | `currentValue − investedAmount`, and as a percentage of `investedAmount` |
| `totalInvested`, `currentPortfolioValue`, `totalProfitLoss`, `portfolioReturnPercentage` | The same, summed over all holdings |

Holdings are updated from executed orders through Kafka, usually within a second. When you sell all shares of a stock, the holding disappears.

### 3.5 Notifications (notification-service)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/v1/notifications` | 🔒 | Your notifications, newest first |

```json
[
  {
    "id": "c856a082-...",
    "userId": "8b58b586-...",
    "type": "ORDER_EXECUTED",
    "title": "ORDER EXECUTED - BUY",
    "message": "Your BUY order for 5 shares of AAPL has been executed at AED 195.30 (total AED 976.50).",
    "createdAt": "2026-10-16T21:10:10.41"
  }
]
```

| Type | When |
|---|---|
| `WELCOME` | After you register |
| `ORDER_EXECUTED` | An order filled |
| `ORDER_FAILED` | An order could not be filled; the message includes the reason |

The last 100 notifications per user are kept, in memory only: they are cleared when notification-service restarts.

### 3.6 Live prices over WebSocket

market-data-service pushes every price change over STOMP. Connect **directly** to the service (the gateway does not route WebSockets):

- Endpoint: `ws://localhost:8082/ws` (SockJS fallback at `http://localhost:8082/ws`)
- Subscribe to: `/topic/price/{SYMBOL}`, for example `/topic/price/AAPL`

Each message is the same JSON as `GET /api/v1/market-data/stocks/{symbol}`. Example with `@stomp/stompjs` in a browser:

```javascript
const client = new StompJs.Client({ brokerURL: "ws://localhost:8082/ws" });
client.onConnect = () =>
  client.subscribe("/topic/price/AAPL", msg => console.log(JSON.parse(msg.body)));
client.activate();
```

### 3.7 Health checks

Every service exposes `GET /actuator/health` on its own port (8080 to 8086), no token needed. A healthy service returns `"status": "UP"` with details of its database, Kafka and disk.

### 3.8 Investment accounts (account-service)

account-service keeps **investment accounts**: many per user, each at a broker. The full reference with every field and error is [account-service-api.md](account-service-api.md); this section shows how to use it.

**Two kinds of accounts**

| brokerName | Kind | Trades | Cash |
|---|---|---|---|
| `PLATFORM` (default), `PAPER` | Platform-traded | `POST /trades/buy`, `/sell` at the live price | Moves with every trade, fee and tax |
| `GROWW`, `ZERODHA`, `BINANCE`, `EXNESS`, `MANUAL` | Tracking | Statement upload or `POST /trades` | Never moved by trades; use deposits, withdrawals or adjustments |

**Walkthrough: track a Zerodha account**

1. Open the account:
   ```json
   POST /api/v1/accounts
   { "accountName": "Zerodha Equity", "accountType": "EQUITY", "brokerName": "ZERODHA", "remarks": "long term" }
   ```
   Your user must be ACTIVE in user-service, otherwise you get 400.
2. Upload a statement (xlsx, xls or csv, max 20 MB); a sample is `docs/postman/samples/zerodha-tradebook.csv`:
   ```bash
   curl -X POST http://localhost:8080/api/v1/accounts/$ACCOUNT/statements/upload \
     -H "Authorization: Bearer $TOKEN" -F "file=@zerodha-tradebook.csv;type=text/csv"
   ```
   The response shows the import result: `status` (`PROCESSED`, `PARTIALLY_PROCESSED`, `FAILED`), trades/charges/taxes imported, skipped rows and warnings.
3. Check the result:
   - `GET /api/v1/holdings/account/{id}`: units, average price, realized profit
   - `GET /api/v1/trades/account/{id}`: the imported trades (`source: IMPORT`)
   - `GET /api/v1/charges/account/{id}/summary?year=2026` and `GET /api/v1/taxes/account/{id}/summary?year=2026`
   - `GET /api/v1/accounts/{id}/performance`: `netProfit = grossProfit − totalCharges − totalTaxes`
4. Record a trade the statement does not have:
   ```json
   POST /api/v1/trades
   { "accountId": "…", "symbol": "TCS", "side": "BUY", "quantity": 2, "buyPrice": 3600,
     "brokerage": 5, "tradeDate": "2026-10-06T10:00:00" }
   ```
   Give both `buyPrice` and `sellPrice` for a closed position (round trip). Quantities may be fractional (crypto, forex lots).
5. Match the cash with the broker: `POST /api/v1/accounts/{id}/adjustments` with a signed `amount` and a `description`.
6. Manage statements: list (`GET /api/v1/accounts/{id}/statements`), download (`GET /api/v1/statements/{sid}/download`), see every import attempt (`GET /api/v1/statements/{sid}/imports`), import again after a mapping change (`POST /api/v1/statements/{sid}/reprocess`), or delete it with everything it imported (`DELETE /api/v1/statements/{sid}`).

Uploading the same file twice returns **409** with the id of the earlier upload. Supported brokers and formats: [statement import](account-service-statement-import.md).

**Walkthrough: trade on a platform account**

```json
POST /api/v1/accounts                     { "accountName": "Main", "accountType": "INDIVIDUAL" }
POST /api/v1/accounts/{id}/deposit        { "amount": 5000 }
POST /api/v1/trades/buy                   { "accountId": "…", "symbol": "AAPL", "quantity": 3 }
GET  /api/v1/accounts/{id}/statement?from=2026-10-01&to=2026-10-31
```

Each BUY debits price × quantity plus fees (brokerage 0.10% min 1.00, platform 0.50, exchange 0.0035%). A SELL also pays 0.05% trade tax and 10% capital gains tax on profit. The statement lists every ledger entry with its running balance.

**Roles**

| Role | Extra rights |
|---|---|
| USER | none: own accounts only |
| ADMIN | read any account (`GET /api/v1/accounts/{id}`, `/accounts/user/{userId}`, …); post dividends (`POST /api/v1/accounts/{id}/dividends`) and interest (`POST /api/v1/accounts/{id}/interest`) |

Your role is in your token; after an administrator promotes you, log in again. A USER calling an ADMIN endpoint gets 403.

## 4. Errors

Every error has the same JSON shape:

```json
{
  "timestamp": "2026-10-16T21:18:18.46",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already registered asha@example.com"
}
```

Validation errors add an `errors` object with one message per field:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": { "symbol": "Stock symbol is required", "quantity": "Quantity must be greater than zero" }
}
```

A failed order adds `orderId`.

| Status | Meaning | Typical causes |
|---|---|---|
| 400 Bad Request | The request is invalid | Missing or invalid fields, e-mail already registered, amount ≤ 0 |
| 401 Unauthorized | Not logged in | No `Authorization` header, invalid or expired token, refresh token used instead of access token, wrong password |
| 403 Forbidden | Logged in, but not allowed | Another user's profile, wallet or order; internal funds endpoints |
| 404 Not Found | It does not exist | Unknown order id, unknown stock symbol, login with an unregistered e-mail |
| 422 Unprocessable Entity | Order saved but not filled | Insufficient wallet balance |
| 502 Bad Gateway | A downstream service is unavailable | user-service or market-data-service down while placing an order |

## 5. Rules and limits

- All orders are **market orders**: they fill immediately at the current price. There are no limit orders, order books or cancellations.
- A BUY needs enough wallet balance for `price × quantity`.
- **Known issue**: a SELL is not yet checked against your holdings, so selling shares you do not own currently succeeds and credits your wallet. Do not rely on this; it will be fixed.
- Access tokens last 15 minutes. There is no refresh endpoint yet; log in again to get a new token.
- Prices are simulated (a random walk of up to ±0.25% per minute), not real market data.
- Notification messages show amounts as "AED", while stock prices are in USD. The wallet has no real currency.

## 6. Testing with Postman

A ready-made collection covering every endpoint and error case is in `docs/postman/`.

### Import

1. In Postman, click **Import** and select both files:
   - `stock-market-system.postman_collection.json`
   - `stock-market-local.postman_environment.json`
2. In the environment selector (top right), choose **Stock Market - Local**.

### Run

Click the collection, then **Run collection** → **Run Stock Market System**. All **102 requests (176 checks)** should pass. Without ADMIN credentials (see below) the 8 requests that need an admin are skipped and the other 94 pass.

The statement upload requests send `docs/postman/samples/zerodha-tradebook.csv`. Newman finds it when run from `docs/postman`; in the Postman app, if the request shows the file as missing, open *Upload statement* → *Body* → *form-data* and select the file once (or set *Settings → Working directory* to `docs/postman`).

You can also run folders one at a time, in order. Folder 1 (*Register*) must run first: it creates a fresh user with a unique e-mail and saves `accessToken`, `refreshToken` and `userId` into the environment. Every later request uses them automatically: the collection uses Bearer auth with `{{accessToken}}`.

| Folder | What it tests |
|---|---|
| 0. Health | Each service's `/actuator/health` is `UP` |
| 1. Users & Auth | Register, login, wrong password, validation errors, profile, add funds |
| 2. Market Data | All stocks, one stock, text list, unknown symbol |
| 3. Orders | BUY 5 and SELL 2 AAPL, get by id, my orders, insufficient funds, unknown symbol, invalid body, not found |
| 4. Portfolio | AAPL holding is 3 (5 bought − 2 sold) |
| 5. Notifications | WELCOME, ORDER_EXECUTED and ORDER_FAILED were all created |
| 6. Gateway security | Missing, garbage and refresh tokens are rejected; a forged `X-User-Id` is ignored; other users' data and internal endpoints are blocked |
| 7. Account Service: platform trading | PLATFORM account: deposit, buy/sell at market price, holdings, withdrawal fee, fund transactions, summary, statement and ledger, fee schedule/preview/charged, tax rules/preview/charged, performance, audit, rename, close (refused with balance; spare account closed), deposit into closed account |
| 8. Account Service: tracking accounts and statements | ZERODHA account: statement upload, duplicate 409, history, download, holdings (INFY 15 @ 1600), charge/tax summaries, manual trade, adjustment, performance, reprocess, delete; then one account and upload per other sample (Groww, Binance, Exness) |
| 9. Account Service: roles | USER blocked from dividends; ADMIN reads another user's account and account list, posts dividend and interest (the user then sees both), cannot deposit into it |
| 10. Direct service calls | Each service on its own port, bypassing the gateway, for debugging |

### ADMIN requests

Folder 9 needs a user with the ADMIN role. Register one (any folder-1-style request or the API), promote it, and put its credentials into the environment variables `adminEmail` and `adminPassword`:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'ops@example.com';
```

*Admin login* then saves `adminAccessToken`. With `adminEmail` empty the ADMIN requests skip themselves.

### Environment variables

| Variable | Default | Set by |
|---|---|---|
| `gatewayUrl` | `http://localhost:8080` | you |
| `userServiceUrl` … `accountServiceUrl` | `http://localhost:8081` … `8086` | you |
| `symbol` | `AAPL` | you (change it to trade another stock) |
| `password` | `Secret123` | you |
| `email` | | *Register* (unique per run) |
| `accessToken`, `refreshToken`, `userId` | | *Register* / *Login* |
| `orderId` | | *Place BUY order* |
| `adminEmail`, `adminPassword` | empty | you (an ADMIN user) |
| `adminAccessToken` | | *Admin login* |
| `platformAccountId`, `trackingAccountId`, `statementId`, `manualTradeId`, `platformTradeId`, `today` | | account-service requests |

### Tips

- If folder 4 or 5 fails right after the orders, send the request again: the portfolio and notifications are filled asynchronously through Kafka.
- After 15 minutes the token expires and requests return 401; run *Login* again.
- To watch the events behind the scenes, open Kafka UI at <http://localhost:8090> and browse the topics `user.registered`, `order.executed`, `order.failed`, `stock.price.updated`, `account.trade.executed` and `account.fund.transaction`.
- Sample statements for every supported broker are in `docs/postman/samples/` (Zerodha, Groww, Binance, Exness); upload them to accounts of the matching broker.
- The collection is generated by `docs/postman/generate_collection.py` (`python generate_collection.py .` in `docs/postman`); change the generator rather than the JSON.
