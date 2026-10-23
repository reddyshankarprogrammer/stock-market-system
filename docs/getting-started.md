# Getting Started

How to start the whole Stock Market System on a developer machine, step by step, and check that every part is working.

- [1. Prerequisites](#1-prerequisites)
- [2. Configure `.env`](#2-configure-env)
- [3. Option A: start everything with one script](#3-option-a-start-everything-with-one-script)
- [4. Option B: start each component manually](#4-option-b-start-each-component-manually)
- [5. Verify the system](#5-verify-the-system)
- [6. Stopping](#6-stopping)
- [7. Troubleshooting](#7-troubleshooting)

---

## 1. Prerequisites

| Tool | Version | Check with |
|---|---|---|
| Java (JDK) | 21 | `java -version` |
| Docker Desktop | with Compose v2 | `docker compose version` |
| Postman (optional) | any recent | for the API tests in [the user manual](user-manual.md) |

Maven does **not** need to be installed: each service ships with a Maven Wrapper (`mvnw` / `mvnw.cmd`).

Free ports needed on `localhost`:

| Port | Used by |
|---|---|
| 3306 | MySQL |
| 6379 | Redis |
| 9092 | Kafka (host listener) |
| 8090 | Kafka UI |
| 8025 / 1025 | Mailpit (development inbox for password-reset emails / SMTP) |
| 8080 to 8086 | the seven Spring Boot services |

## 2. Configure `.env`

All configuration lives in `.env` at the repo root. Docker Compose reads it automatically, and every service imports it at startup (`spring.config.import: optional:file:../.env`).

The values that must be set:

| Key | Purpose |
|---|---|
| `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_ROOT_PASSWORD` | Database name and credentials (MySQL container is created with these) |
| `JWT_SECRET` | HMAC key used by user-service to sign tokens and by api-gateway to verify them. At least 32 characters. There is no default, so a missing value stops both services at startup |
| `*_SERVICE_PORT`, `API_GATEWAY_PORT` | Port of each service (defaults: 8080 to 8086; `ACCOUNT_SERVICE_PORT=8086`) |
| `ACCOUNT_STATEMENTS_DIR` (optional) | Where account-service stores uploaded statements (default `uploads/statements` under `account-service/`) |
| `KAFKA_PORT`, `REDIS_PORT`, `MYSQL_PORT` | Host ports of the infrastructure |

> The host names in `.env` (`MYSQL_HOST=mysql`, `KAFKA_BOOTSTRAP_SERVERS=kafka:29092`) are Docker container names. They only resolve inside the Docker network. Services started on your machine ignore them and use `localhost`; they are only used when a service runs with the `docker` profile.

Never commit real secrets in `.env`.

## 3. Option A: start everything with one script

From the repo root in PowerShell:

```powershell
.\scripts\start-all.ps1
```

The script:

1. Runs `docker compose up -d` (MySQL, Redis, Kafka, Kafka UI).
2. Builds every service with `mvnw package -DskipTests`.
3. Starts all seven jars in the background, each from its own folder so `../.env` resolves.
4. Waits until every `/actuator/health` reports `UP` and prints the result.

Logs are written to `logs\<service>.log` and process ids to `logs\pids.txt`.

To restart without rebuilding: `.\scripts\start-all.ps1 -NoBuild`.

> If PowerShell refuses to run the script, allow local scripts for your user once:
> `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

## 4. Option B: start each component manually

Use this when you want to see each service's console output, or run one service from your IDE.

### Step 1: Start Docker Desktop

Open Docker Desktop and wait until it shows **Engine running**. Check with:

```powershell
docker info --format "{{.ServerVersion}}"
```

### Step 2: Start the infrastructure

```powershell
cd C:\Repo\stock-market-system
docker compose up -d
docker compose ps
```

Wait until `mysql` shows `healthy` (about 20 to 30 seconds on first start). Kafka UI is now at <http://localhost:8090>.

### Step 3: Start the services, in this order

Open one terminal per service. In each one, `cd` into the service folder and run it with the wrapper:

```powershell
cd user-service
.\mvnw.cmd spring-boot:run
```

(On macOS/Linux use `./mvnw spring-boot:run`.)

| Order | Service | Folder | Wait for log line |
|---|---|---|---|
| 1 | user-service | `user-service` | `Started UserServiceApplication` |
| 2 | market-data-service | `market-data-service` | `Started MarketDataServiceApplication` and `Loaded 6 stocks into price cache` |
| 3 | order-service | `order-service` | `Started OrderServiceApplication` |
| 4 | portfolio-service | `portfolio-service` | `Started PortfolioServiceApplication` |
| 5 | notification-service | `notification-service` | `Started NotificationServiceApplication` |
| 6 | account-service | `account-service` | `Successfully applied … migrations` (first start) and `Started AccountServiceApplication` |
| 7 | api-gateway | `api-gateway` | `Started ApiGatewayApplication` |

Why this order: order-service calls user-service and market-data-service, portfolio-service calls market-data-service, account-service calls user-service (to validate users) and market-data-service (prices), and the gateway forwards to all of them. Strictly, each service starts on its own; the order only matters once you send requests.

On its first start account-service runs its Flyway migrations (V1 to V11) into the shared database; later starts only apply new versions.

On the first start, market-data-service creates the `stocks` table and seeds six stocks (AAPL, MSFT, GOOG, AMZN, TSLA, NVDA). Prices then change once a minute.

Alternative to `spring-boot:run`: build once with `.\mvnw.cmd package -DskipTests`, then run `java -jar target\<name>-0.0.1-SNAPSHOT.jar` **from the service folder** (the `.env` import is a relative path).

### Running from an IDE

Run each `*Application` class with the **working directory set to the service folder** (for example `C:\Repo\stock-market-system\order-service`). Otherwise `../.env` is not found and the service fails with an unresolved placeholder such as `JWT_SECRET` or `MYSQL_DATABASE`.

## 5. Verify the system

### Health checks

```powershell
8080..8086 | ForEach-Object { "{0} {1}" -f $_, (Invoke-RestMethod "http://localhost:$_/actuator/health").status }
```

All seven should print `UP`.

### Swagger (account-service)

Open <http://localhost:8086/swagger-ui.html>. Requests from Swagger go straight to the service: add the header `X-User-Id` (any user id you created) and, for dividend/interest posting, `X-User-Role: ADMIN`.

### Create an ADMIN user (optional)

Roles live in user-service (`USER` by default). Register a user, promote them, then log in again so the new token carries the role:

```powershell
docker exec mysql sh -c "mysql -uroot -p`$MYSQL_ROOT_PASSWORD -e ""UPDATE `$MYSQL_DATABASE.users SET role='ADMIN' WHERE email='admin@example.com'"""
```

### Smoke test through the gateway

```powershell
# Prices are public
Invoke-RestMethod http://localhost:8080/api/v1/market-data/stocks/AAPL

# Register and use the token
$body = @{ email = "me$(Get-Random)@example.com"; password = "Secret123"; firstName = "Asha"; lastName = "Rao" } | ConvertTo-Json
$auth = Invoke-RestMethod -Method Post http://localhost:8080/api/v1/users/register -ContentType "application/json" -Body $body
Invoke-RestMethod http://localhost:8080/api/v1/users/me -Headers @{ Authorization = "Bearer $($auth.accessToken)" }
```

### Full test suite in Postman

Import `docs/postman/stock-market-system.postman_collection.json` and `docs/postman/stock-market-local.postman_environment.json`, select the **Stock Market - Local** environment and use **Run collection**. All 102 requests should pass (94 when no ADMIN user is configured; the admin requests are skipped). See the [user manual](user-manual.md#6-testing-with-postman).

The same collection runs from the command line with Newman:

```powershell
cd docs\postman
npx newman run stock-market-system.postman_collection.json -e stock-market-local.postman_environment.json --delay-request 300 `
    --env-var "adminEmail=admin@example.com" --env-var "adminPassword=Secret123"
```

### Unit tests

From any service folder:

```powershell
.\mvnw.cmd test
```

user-service, order-service, notification-service and api-gateway have unit tests that need no infrastructure. The `contextLoads` tests of market-data-service and portfolio-service start the full application against the docker-compose infrastructure.

account-service has 44 tests: unit tests for the fee, tax, ledger and holding engines and the statement parsers, plus integration tests that start their **own** MySQL 8.0 and Kafka containers with Testcontainers (Docker must be running; the first run downloads the images). They never touch the development database.

## 6. Stopping

```powershell
.\scripts\stop-all.ps1          # stop the seven services
.\scripts\stop-all.ps1 -Infra   # also stop MySQL, Redis, Kafka, Kafka UI
```

Services started with `spring-boot:run` stop with `Ctrl+C` in their terminal. Infrastructure stops with `docker compose down`; data survives in Docker volumes. `docker compose down -v` deletes all data (users, orders, holdings, Kafka topics).

## 7. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `docker ... dockerDesktopLinuxEngine ... cannot find the file` | Docker Desktop is not running | Start Docker Desktop and wait for *Engine running* |
| `Could not resolve placeholder 'JWT_SECRET'` (or `MYSQL_DATABASE`) | Service started from the wrong working directory, so `../.env` was not found | Start it from its own folder, or set the IDE working directory to the service folder |
| `Public Key Retrieval is not allowed` | MySQL 8 auth over a non-SSL connection | Already handled by `allowPublicKeyRetrieval=true` in each JDBC URL; check you are on the current `application.yaml` |
| `Communications link failure` | MySQL not ready yet | Wait until `docker compose ps` shows mysql `healthy`, then restart the service |
| `Port 808x was already in use` | An old instance is still running | `.\scripts\stop-all.ps1`, or find it with `Get-NetTCPConnection -LocalPort 8083` and stop that process |
| Every gateway call returns 401 *Invalid or expired token* | user-service and api-gateway use different `JWT_SECRET` values, or the token expired (15 minutes) | Make sure both read the same `.env`; log in again for a new token |
| Order fails with 404 *Stock not found* | Symbol not in the seeded list, or market-data-service restarted with an empty table | Use AAPL, MSFT, GOOG, AMZN, TSLA or NVDA; check the market-data log for `Loaded 6 stocks` |
| Portfolio or notifications are empty right after an order | They are filled asynchronously from Kafka | Wait a second and call again; check the topic in Kafka UI |
| Kafka warnings `Connection to node -1 could not be established` | Kafka container not up yet | `docker compose ps`; the service reconnects automatically |
| account-service: `Validate failed: Migration checksum mismatch` | An applied Flyway script was edited | Revert the script and add a new `V<n>__…sql` instead |
| account-service: `Schema-validation: missing column` | Entity changed without a migration | Add a migration for the column |
| Opening an account returns 502 *User service unavailable* | user-service is down or slow | Start user-service; check `logs/user-service.log` |
| Statement upload returns 400 *Statements cannot be imported for PLATFORM accounts* | Upload goes to a tracking account (GROWW, ZERODHA, BINANCE, EXNESS) | Create the account with `brokerName` set |
| Statement status `PARTIALLY_PROCESSED` / `FAILED` | Rows or columns the parser did not recognize | `GET /api/v1/statements/{id}/imports` shows the warnings; adjust `account.import.brokers.*` and reprocess |
| `start-all.ps1` stops right after "Starting infrastructure..." | Older copy of the script treated Docker progress output as an error | Use the current script (it checks exit codes instead) |
