# Account Service: Sequence Diagrams

All client calls pass the api-gateway, which validates the JWT and forwards `X-User-Id` / `X-User-Role`. The gateway is left out below the first diagram for brevity.

## Account creation

```mermaid
sequenceDiagram
    participant C as Client
    participant G as api-gateway
    participant A as account-service
    participant U as user-service
    participant DB as MySQL

    C->>G: POST /api/v1/accounts + Bearer JWT
    G->>G: verify JWT, set X-User-Id, X-User-Role
    G->>A: forward
    A->>U: GET /api/v1/users/me (Feign, X-User-Id forwarded)
    alt user unknown / not ACTIVE / user-service down
        U-->>A: 404 / status != ACTIVE / error
        A-->>C: 404 / 400 / 502
    else ACTIVE
        U-->>A: UserDto (status ACTIVE)
        A->>DB: insert investment_accounts (balance 0, ACTIVE, broker)
        A->>DB: insert account_audit_logs (CREATE, request + response JSON)
        A-->>C: 201 AccountResponse
    end
```

## Statement upload

```mermaid
sequenceDiagram
    participant C as Client
    participant S as AccountStatementService
    participant DB as MySQL
    participant FS as File storage
    participant I as StatementImportService

    C->>S: POST /accounts/{id}/statements/upload (multipart file)
    S->>S: validate extension, magic bytes, size <= 20 MB
    S->>S: SHA-256 checksum
    rect rgb(235, 245, 255)
    note over S,FS: transaction 1
    S->>DB: account owned by caller, ACTIVE, broker with a parser?
    S->>DB: same checksum already uploaded? -> 409
    S->>DB: insert account_statements (UPLOADED)
    S->>FS: write <accountId>/<statementId>.<ext>
    S->>DB: audit STATEMENT_UPLOAD
    end
    S->>I: process(statementId)
    I-->>S: import history
    S-->>C: 201 AccountStatementResponse + latestImport
```

## Statement processing

```mermaid
sequenceDiagram
    participant I as StatementImportService
    participant DB as MySQL
    participant FS as File storage
    participant R as TabularDataReader
    participant PF as ParserFactory
    participant P as Broker parser
    participant H as HoldingService

    I->>DB: tx: status PROCESSING
    I->>PF: getParser(broker)
    I->>FS: open file
    I->>R: read csv / xls / xlsx -> TabularData
    I->>P: parse(TabularData)
    P-->>I: ParsedStatement (trades, charges, taxes, P&L summary, warnings)
    alt read/parse failed
        I->>DB: tx: history FAILED + error, statement FAILED, audit
    else parsed
        rect rgb(235, 245, 255)
        note over I,H: transaction 2 (all or nothing)
        I->>DB: delete what this statement imported before
        loop each trade row
            I->>DB: skip if externalTradeId already imported
            I->>DB: insert account_trades (source IMPORT)
            I->>DB: insert per-trade charges / tax
        end
        I->>DB: insert statement-level charges and taxes
        I->>H: rebuildHoldings(account)
        I->>DB: insert statement_import_history (SUCCESS / PARTIAL)
        I->>DB: statement PROCESSED / PARTIALLY_PROCESSED, period
        I->>DB: audit STATEMENT_PROCESS (history JSON)
        end
    end
```

## Trade import (one row)

```mermaid
sequenceDiagram
    participant P as Parser
    participant I as Importer
    participant DB as MySQL

    P->>P: find header row (aliases from application.yaml)
    P->>P: map cells: symbol, side, quantity, price, date, id, charges
    alt EXECUTION row
        P-->>I: Trade(BUY/SELL, quantity, price, brokerage, fee, tax)
        I->>DB: account_trades: price, gross = price x qty, net = gross +/- fees, taxes
    else CLOSED_POSITION row
        P-->>I: Trade(long/short, buy price, sell price, P&L)
        I->>DB: account_trades: realized = P&L - fees, net = realized - tax
    else invalid row
        P->>P: skip, warning "Row N: reason"
    end
    I->>DB: account_charges (BROKERAGE / OTHER), account_taxes (TRADE_TAX)
```

## Holding calculation

Platform accounts update holdings incrementally; tracking accounts replay their whole history.

```mermaid
sequenceDiagram
    participant T as TradeService / Importer
    participant H as HoldingService
    participant DB as MySQL

    alt PLATFORM / PAPER trade
        T->>H: updateHoldingAfterBuy(qty, price)
        H->>H: avg = (q x avg + qty x price) / (q + qty)
        T->>H: updateHoldingAfterSell(qty, realized)
        H->>H: q -= qty, cost = avg x q, realized += profit
    else tracking account (manual trade, import, statement delete)
        T->>H: rebuildHoldings(account)
        H->>DB: load holdings (reset to zero) and all trades by date
        loop each trade
            alt EXECUTION BUY
                H->>H: add units, recompute average
            else EXECUTION SELL
                H->>H: realized = (price - avg) x units - fees (stored on the trade)
                H->>H: warn if selling more than known
            else CLOSED_POSITION
                H->>H: realized += trade.realizedProfit (no unit change)
            end
        end
        H->>DB: save holdings
    end
```

## Account performance calculation

```mermaid
sequenceDiagram
    participant C as Client
    participant P as PerformanceService
    participant H as HoldingService
    participant M as market-data-service
    participant DB as MySQL

    C->>P: GET /accounts/{id}/performance
    P->>DB: account readable by caller (owner or ADMIN)?
    P->>H: valueOpenPositions(account)
    H->>M: current price per symbol (missing -> last known / none)
    H-->>P: holdings with unrealized P&L
    P->>DB: SUM(realized_profit), SUM(fees inside realized)
    P->>DB: SUM(account_fees) + SUM(account_charges), SUM(account_taxes)
    P->>P: gross = realized + unrealized + fees inside realized
    P->>P: net = gross - charges - taxes
    P->>DB: upsert account_performance
    P-->>C: PerformanceResponse
```

## Platform trade (BUY)

```mermaid
sequenceDiagram
    participant C as Client
    participant T as TradeService
    participant M as market-data-service
    participant DB as MySQL
    participant L as LedgerService
    participant K as Kafka

    C->>T: POST /trades/buy
    T->>M: current price
    T->>DB: SELECT account FOR UPDATE (owner, ACTIVE, PLATFORM/PAPER)
    T->>T: gross, fees (fee engine), taxes (tax engine)
    T->>DB: insert account_trades
    T->>L: debit TRADE_BUY gross, FEE, TAX
    T->>DB: account_fees, account_taxes, holding update
    T->>DB: audit BUY, FEE_CALCULATION, TAX_CALCULATION, HOLDING_UPDATE
    T-->>C: 201 TradeResponse (balanceAfter)
    T--)K: after commit: account.trade.executed
```
