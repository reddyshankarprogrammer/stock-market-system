# Changelog

All notable changes to the Stock Market System. Dates are when the change was made.

## 2026-10-24: forgot and reset password with an email service

### Features

| Feature | Where |
|---|---|
| `POST /api/v1/users/forgot-password` `{email}`: **always 202** with the same message, whether or not the email is registered. For a real, unblocked account it emails a single-use link; the email is sent in the background so response time does not reveal anything either. A 60-second per-user cooldown stops inbox flooding, and a newer link voids older ones. | `user-service` `PasswordResetService`, `UserController` |
| `POST /api/v1/users/reset-password` `{token, newPassword}`: changes the password (BCrypt), spends the token, voids other pending links. One `400 This reset link is invalid or has expired` for unknown, used, expired or blocked-account tokens (typed `InvalidResetTokenException`). | same |
| The token is 256 random bits; **only its SHA-256 hash is stored** (`password_reset_tokens`), valid 30 minutes, single use. | new table `password_reset_tokens` |
| A reset sets `users.password_changed_at`; **refresh tokens issued before it are refused**, so old sessions end. | `UserServiceImpl.refresh`, `JwtService.extractIssuedAt` |
| Outgoing email through Spring Mail; failures are logged and never change an API answer; works without mail configured (the sender is looked up lazily, tests do not set a host). | `MailService`, `SmtpMailService` |
| Both paths are public in the gateway. | `api-gateway` `JwtAuthFilter.PUBLIC_PATHS` |
| **Mailpit** (development inbox) added to docker compose: SMTP 1025, inbox at http://localhost:8025. Nothing leaves the machine. | `docker-compose.yml`, `.env.example` (`MAIL_HOST`, `MAIL_PORT`, `MAILPIT_UI_PORT`, `MAIL_FROM`, `APP_BASE_URL`) |
| Web app: working Forgot Password and Reset Password pages. The confirmation is identical for every email; the reset page reads the token once and removes it from the address bar, and is served with `Referrer-Policy: no-referrer` and `Cache-Control: no-store`. | `web-app` `ForgotPasswordForm`, `ResetPasswordForm`, `/api/auth/forgot-password`, `/api/auth/reset-password`, `next.config.ts` |

### Compatibility and limits

- New endpoints, one new table and two nullable additions (`users.password_changed_at`); nothing existing changed. Hibernate `update` creates the table.
- **No production email provider yet.** Until `MAIL_HOST`, `MAIL_FROM` and `APP_BASE_URL` are set for user-service in `k8s/user-service/configmap.yaml` (and credentials in its Secret), reset emails fail in the cluster and are only logged. Not changed in `k8s/` here because the provider and the public web address are undecided.
- An access token already issued stays valid until it expires (at most 15 minutes) after a reset. Expired reset tokens are never deleted. No rate limiting on `/reset-password`.
- `axllent/mailpit:latest` is not pinned (same as the existing `kafka-ui:latest`).

### Changes to existing files

| File | Change |
|---|---|
| `user-service/README.md`, `api-gateway/README.md`, `docs/user-manual.md`, `docs/technical-documentation.md` (ER diagram, ports, security, public paths), `docs/getting-started.md`, root `README.md`, root `CLAUDE.md` (infrastructure line, gaps) | Endpoints, tables, configuration, Mailpit |
| `docs/postman/generate_collection.py` and the generated collection | 5 new requests (forgot, same answer for an unknown email, invalid email, invalid token, short password); 106 → 111 requests |

### Tests

user-service **61 tests pass** (18 new: real BCrypt change of password, hash-only storage, single use, expiry, blocked accounts, cooldown, older links voided, refresh refused after a reset, controller checks, SMTP sender, and a test that sends a real message over a socket to an in-process SMTP server). api-gateway 10 tests pass (assertions added). Web app: **38 of 38** browser checks against the mock gateway (7 new). Not run on MySQL, through the real gateway, with Mailpit in Docker, or with Newman.

## 2026-10-24: refresh-token endpoint; web app sessions renew themselves

### Features

| Feature | Where |
|---|---|
| `POST /api/v1/users/refresh` `{refreshToken}`: returns a new access token (role and email read from the database) and the same refresh token. Rejects bad signature, expired, tampered or access tokens, unknown users and blocked statuses with one `401 Invalid refresh token`. | `user-service` `UserServiceImpl.refresh`, `RefreshRequest`, `InvalidRefreshTokenException` (typed, mapped in `GlobalExceptionHandler`) |
| `/api/v1/users/refresh` is a **public path** (the access token has expired; the refresh token is in the body). Refresh tokens are still rejected as Bearer tokens everywhere else. | `api-gateway` `JwtAuthFilter.PUBLIC_PATHS` |
| The web app renews the access cookie from the refresh cookie when it expires or user-service answers 401, retries once, and ends the session only when the refresh token is really invalid. A gateway outage during renewal does not log anyone out. "Remember me" now keeps the refresh cookie for 7 days (otherwise it ends with the browser session). | `web-app` `app/api/auth/me`, `lib/auth/server.ts`, `proxy.ts` |

### Compatibility and limits

- New endpoint only; no existing request or response changed.
- Refresh tokens stay stateless: not rotated, not revocable, no logout endpoint, so a stolen token works until it expires or the user's status blocks it.
- Found while working: **login does not check the user status** (a SUSPENDED user can still log in). Not changed here; listed in the gaps in the root `CLAUDE.md`, the user-service README and the technical documentation.
- No rate limiting on `/login` or `/refresh`.

### Changes to existing files

| File | Change |
|---|---|
| `user-service/README.md`, `api-gateway/README.md`, `docs/user-manual.md`, `docs/technical-documentation.md` (section 9 and the gaps table), root `CLAUDE.md` (gaps) | Endpoint, public path and the corrected limitations |
| `docs/postman/generate_collection.py` and the generated collection | 4 new requests (refresh, access token rejected, garbage rejected, missing token); 102 → 106 requests |

### Tests

user-service 43 tests pass (10 new: real-token round trip, access token refused, expired, other secret, tampered, unknown user, blocked statuses, controller 200/401/400); api-gateway 10 tests pass (1 new). Web app: 31 of 31 browser checks against the mock gateway (6 new: renewal from the refresh cookie, 401 retried once, invalid token ends the session, blocked user, outage keeps the session, cookie lifetimes). Not run through the real gateway, MySQL or Newman.

## 2026-10-24: user-service stores profile fields collected at registration

### Features

| Feature | Where |
|---|---|
| `POST /api/v1/users/register` accepts optional `mobileCountryCode`, `mobileNumber`, `gender` (`MALE`/`FEMALE`), `country` (ISO alpha-2) and `termsVersion`; validated when present; country code and number must come together. The server records `termsAcceptedAt` from its own clock. | `user-service` `RegisterRequest`, `UserServiceImpl` |
| `GET /api/v1/users/me` (and the other `UserResponse` endpoints) return the six new fields, `null` for users registered earlier | `UserResponse` (new fields last) |
| New nullable columns on `users` (Hibernate `update`, no data migration needed) | `entity/User`, new `entity/Gender` |

### Compatibility

- All new request fields are optional, so existing clients and the old Postman request keep working.
- The `user.registered` Kafka map is unchanged on purpose: mobile, gender and country are personal data no consumer needs. A test pins the key set.
- account-service ignores unknown fields in its `UserDto`, so it needs no change.

### Changes to existing files

| File | Change |
|---|---|
| `user-service/README.md`, `docs/user-manual.md`, `docs/technical-documentation.md` | API fields, data model and ER diagram; test count 23 → 33 |
| `docs/postman/generate_collection.py` and the generated collection | Register sends the fields; Get my profile asserts them |
| `web-app/app/api/auth/register/route.ts` | Forwards the fields; the "not saved yet" notice is gone |

### Tests

5 new user-service tests (fields saved, acceptance time from the server, no fields still works, Kafka contract unchanged, validation of bad values); 33 tests pass. The web app's 25 browser checks run against a mock gateway and now verify the exact forwarded body. Not yet run against MySQL and the real gateway.

## 2026-10-18: GitOps bootstrap fixes (found on the first K3s install)

### Fixes

| Problem | Fix | Where |
|---|---|---|
| Root application deadlocked: `namespaces` (wave -10) waited for the `stock-market` AppProject, which had wave 0 | AppProject gets `sync-wave: "-20"` | `k8s/argocd/project.yaml` |
| `mysql` application stuck in Progressing: PVC `mysql-backup` stays Pending until the 02:00 CronJob (local-path binds on first consumer), blocking every later wave | Argo CD health check treats a Pending PVC as Healthy (Lost stays Degraded) | `k8s/argocd/argocd-values.yaml` (re-run `scripts/install-argocd.sh` to apply) |
| `monitoring` application permanently OutOfSync and re-synced every 5 minutes: the Prometheus Operator defaults `action: replace` on relabelings that omit it | Relabelings of the ServiceMonitor `stock-market-services` and PodMonitor `kafka` set `action: replace` explicitly | `k8s/monitoring/service-monitors.yaml` |

## 2026-10-18: Whole system in Docker Compose

| File | Change |
|---|---|
| `docker-compose.yml` | user, market-data, order, portfolio, notification services and api-gateway added to the opt-in `app` profile: `docker compose --profile app up -d --build` runs the complete system on one machine (only the gateway port 8080 is published) |

## 2026-10-18: Kubernetes platform, CI/CD and GitOps

### Features

| Feature | Where |
|---|---|
| K3s install, upgrade, uninstall, health check (Ubuntu 24.04) | `scripts/install-k3s.sh`, `upgrade-k3s.sh`, `uninstall-k3s.sh`, `cluster-health-check.sh` |
| One-command platform bootstrap; secrets from `.env` | `scripts/bootstrap-platform.sh`, `create-secrets.sh` |
| Kubernetes manifests for all 8 services (Deployment, Service, ConfigMap, Secret template, HPA, PDB, ServiceAccount) | `k8s/<service>/` |
| MySQL 8.4 with exporter, Valkey (Redis), Strimzi Kafka (KRaft) with 15 declared topics | `k8s/mysql`, `k8s/redis`, `k8s/kafka` |
| F5 NGINX Ingress, cert-manager (Let's Encrypt + local CA) | `k8s/ingress`, `scripts/install-ingress.sh` |
| Argo CD app of apps (15 applications, sync waves, AppProject guard rails) | `k8s/argocd`, `scripts/install-argocd.sh` |
| Prometheus, Alertmanager, Grafana: 12 dashboards, 20 alerts | `k8s/monitoring`, `scripts/install-monitoring.sh` |
| Loki + Grafana Alloy; search by userId, accountId, tradeId, statementId | `k8s/logging`, `scripts/install-logging.sh` |
| Network policies, RBAC, Pod Security Standards, quotas, image pull secret | `k8s/security`, `k8s/namespaces` |
| Nightly compressed MySQL backups (30-day retention), restore script, optional off-site copy | `k8s/mysql/backup-cronjob.yaml`, `scripts/restore-database.sh` |
| GitHub Actions: CI, CD with manual approval, security scans, releases | `.github/workflows/` |
| Production Dockerfiles: Java 21, multi-stage, layered, distroless, non-root | `<service>/Dockerfile` |

### Changes to existing files

| File | Change |
|---|---|
| every service `pom.xml` | `micrometer-registry-prometheus` (runtime) for `/actuator/prometheus` |
| `account-service/Dockerfile` | replaced by the shared production layout; statements in `/data/statements` |
| `docker-compose.yml` | `account-statements` volume for the account-service container |
| `README.md`, `CLAUDE.md` | Kubernetes, CI/CD and operations sections |

### Notes

- Replaced end-of-life components the request named: ingress-nginx (retired March 2026) by F5 NGINX Ingress Controller, Promtail (EOL 2026-03-02) by Grafana Alloy; MySQL 8.0 (EOL April 2026) by 8.4 LTS in Kubernetes; Redis by Valkey (BSD license).
- The 9 domain topics (`account-created` ... `notification-created`) are created but have no producer or consumer yet; the services keep using their existing topics.
- New `.env` keys for Kubernetes: `MYSQL_APP_USERNAME`, `MYSQL_APP_PASSWORD`, `MYSQL_EXPORTER_PASSWORD`, `REDIS_PASSWORD`, `GRAFANA_ADMIN_*`, `ALERT_SLACK_WEBHOOK_URL`, `GHCR_USERNAME`, `GHCR_TOKEN` (see `.env.example`).

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
