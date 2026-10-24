# Changelog

All notable changes to the Stock Market System. Dates are when the change was made.

## 2026-10-24: launch list on the landing page (waitlist)

### Features

| Feature | Where |
|---|---|
| `POST /api/v1/users/waitlist` (public): email and consent, lower-case unique address with the time of consent, the same 202 for a new and a listed email, address never logged; new table `waitlist_entries`; public path in the gateway | `user-service`, `api-gateway` |
| Landing page section "Join the launch list": validation, consent link to the privacy policy, honeypot, `/api/waitlist` route with a per-pod rate limit (429 and `Retry-After`) | `web-app/components/WaitlistForm.tsx`, `web-app/app/api/waitlist/` |
| 5 new browser checks (87), 5 new user-service tests (88), gateway filter test, 3 Postman requests | `web-app/e2e/`, `user-service`, `docs/postman` |

### Known limits

The list is write-only (no export, no reading, no email). The rate limit counts per web pod and is not a security boundary. The privacy text that should cover the list is still a draft for legal review. A user-service rate limit and a shared limiter are open.

## 2026-10-24: MVP screens (accounts, statement upload, trades, dashboard, portfolio)

### Features

| Feature | Where |
|---|---|
| Accounts: open an account, account page with summary figures, broker statement upload with the import result, delete a statement | `web-app/components/accounts/` |
| Trades: table with date, symbol and side filters, sortable columns, paging, trade detail, trade entry (market price on platform accounts, manual on tracking accounts) | `web-app/components/trades/`, `web-app/lib/trades.ts` |
| Dashboard: key figures and the profit and loss calendar (per day, month total, all-time total checked against the account's realised profit) | `web-app/components/dashboard/`, `web-app/lib/pnl.ts` |
| Portfolio: open positions with prices, refresh every 10 seconds | `web-app/components/portfolio/` |
| BFF: raw multipart relay for the statement upload path only (same origin, 60 s), `DELETE` without a JSON content type; ingress accepts 25 MB for `/api/gw` | `web-app/app/api/gw/`, `k8s/ingress/web-app-ingress.yaml` |
| 17 new mock browser checks (82 in total) with a mock account-service, and `e2e:mvp:real` (9 checks against the real stack) | `web-app/e2e/` |

### Bugs found by testing and fixed

| Bug | Fix |
|---|---|
| **account-service: `GET /holdings` answered 409** ("changed by another request") when two reads overlapped (a polling page plus a refresh): the read stored the live price through the versioned entity | The price snapshot is stored by a version-free update (`HoldingRepository.updatePriceSnapshot`), the entity is detached first; 12 parallel reads all answer 200; unit test added (45 account-service tests pass) |
| A date filter with only `from` made the Trades page empty: account-service answers 400 ("give both from and to, or neither") and the mock had accepted it | The page sends both ends or none and filters one-sided ranges itself; the mock now refuses a lone end like the real service |
| The trade detail had no label for the real source value `IMPORT` (the mock had used `STATEMENT`) | Label for `IMPORT`; the mock uses the real value |
| `DELETE` through the BFF was refused with 415 (it demanded a JSON content type, a DELETE has no body) | Only methods with a body need one |
| A failed 10-second price refresh blanked the holdings table | The last prices stay on screen with a notice |
| `usePathname` in the navigation broke the build for the dynamic route `/accounts/[id]` | Navigation wrapped in `Suspense` |
| One test cleared the access cookie while the dashboard's own data requests were still running, so three parallel calls each renewed the session | The test waits for the page to settle; sequential renewal is by design (profile first) |
| OpenAPI document: the upload's 400 and 409 are typed as a statement but are error bodies; `source` is `IMPORT`, not `STATEMENT` | Handled in the client; the spec is unchanged (noted for the springdoc clean-up) |

### Known limits

No websocket: prices are polled. The calendar is computed in the browser from the trades. Statement parsers have been exercised with a Zerodha tradebook only (the other brokers' layouts are still unconfirmed with real files). Money in the browser tests is compared as formatted text. Accounts cannot yet be renamed, closed or funded from the UI (deposit and withdraw exist in the API).

Cleanup of the real-stack test data (counts first): users `e2e-mvp-%@example.com`, then their rows in `account_*` tables, `statement_import_history`, `account_audit_logs`, `user_preferences`, and the directories `account-service/uploads/statements/<account id>`.

## 2026-10-24: settings (General, Notifications, Security, Downloads, About) and change password

### Features

| Feature | Where |
|---|---|
| `GET` / `PUT /api/v1/users/me/preferences` (theme, language, notification channels, export format): partial saves, defaults without a row, new table `user_preferences` (created by Hibernate, nothing existing touched) | `user-service` |
| `POST /api/v1/users/me/password`: checks the current password, sets `password_changed_at` so older sessions end, answers with fresh tokens so the session that changed it carries on. Wrong current password is **422**, so the web app does not mistake it for an ended session | `user-service` |
| Settings screens: category list, General, Notifications, Security (change password; two-factor, passkeys, sessions and login history listed as not available yet), Downloads, About; server-saved theme that follows the user across devices | `web-app/app/(app)/settings/`, `web-app/components/settings/` |
| 9 new browser checks on the mock (65 in total), `e2e:settings:real` (8 checks on the real stack), 7 new user-service tests (83), 6 new Postman requests | `web-app/e2e/`, `user-service`, `docs/postman` |

### Bugs found by testing and fixed

| Bug | Fix |
|---|---|
| The settings category row made the page 505 px wide on a 320 px phone (a grid item grows to its content) | `minmax(0,1fr)` column and `min-w-0` on the row, which now scrolls inside itself |
| The shell's new settings query and the profile query both renewed an expired session at the same moment (two refresh calls) | The settings query waits for the profile query |
| A controlled radio showed the old choice for a moment after a click (React restores it unless state changes in the same event) | The chosen value is kept in local state until the save settles |
| A test looked for the text "Saved" and also matched descriptions containing "saved" | Exact text match |

### Known limits

Sessions and devices, two-factor, passkeys and login history need Keycloak and are not built. Push and email alert choices are stored, but nothing sends push or alert emails yet. After a password change the refresh cookie becomes a session cookie (the web app cannot tell whether Remember me was ticked). Refresh tokens are compared with the change time in whole seconds, so a session started in the same second as a password change survives it.

## 2026-10-24: app shell and typed API client

### Features

| Feature | Where |
|---|---|
| App shell: sidebar from 1024 px, top bar and bottom navigation below; skeleton pages for Trades, Portfolio and Settings; `proxy.ts` protects them | `web-app/app/(app)/`, `web-app/components/shell/` |
| Typed API client: account-service OpenAPI saved as `openapi/account-service.json`, TypeScript types generated with openapi-typescript, openapi-fetch client and TanStack hooks, `npm run api:spec:account` and `api:types`, CI check that the types are current | `web-app/lib/api/`, `web-app/scripts/fetch-openapi.mjs` |
| `/api/gw/[...path]` BFF pass-through with a path allow-list, token renewal and same-origin writes | `web-app/app/api/gw/` |
| Accounts page on the typed client | `web-app/components/accounts/` |
| 10 new browser checks on the mock (56 in total) and `e2e:accounts:real` (8 checks on the real stack) | `web-app/e2e/` |

### Bugs found and fixed

| Bug | Fix |
|---|---|
| `GET /api/v1/accounts/user/{userId}` documented only a 403 response, so the generated types put the account list under the error status | `@ApiResponse(200)` added in account-service `AccountController`; the other 39 operations were already correct (44 account-service tests pass) |
| `zod` 3.25.76 is flagged high by `npm audit` (memory exhaustion on oversized collections) and **no 3.x release fixes it**; the Trivy filesystem scan in CI would have failed on it | Moved to zod 4.6.7 (`errorMap` became `error` in four enum schemas); `npm audit` shows 0 vulnerabilities; all 56 browser checks pass; the Dependabot ignore for zod and resolvers majors was removed |

## 2026-10-24: web app deployment (Docker, Kubernetes, Argo CD, CI)

### Features

| Feature | Where |
|---|---|
| `web-app/Dockerfile`: standalone Next.js server on distroless Node 24, uid 65532, read-only root; `GET /api/health` probe that does not depend on the gateway | `web-app/` |
| `k8s/web-app/` (Deployment, Service, ConfigMap with `API_GATEWAY_URL`, HPA 2 to 5, PDB, ServiceAccount), ingress `app.stockmarket.local`, NetworkPolicy from the ingress controller, Argo CD application | `k8s/` |
| CI job `web-app` (ESLint, type check, build, 46 browser checks on the standalone server); web-app added to the image matrix, `cd.yml`, `release.yml` and Dependabot (npm and docker); Grafana board "Stock Market / Web App" | `.github/`, `k8s/monitoring/` |

### Bugs found by testing and fixed

| Bug | Fix |
|---|---|
| The same-origin check compared the full `Origin` with the server's own origin, which would have returned 403 on every sign-in behind a TLS ingress (the server sees `http`, the browser sends `https`) | It compares hosts (`X-Forwarded-Host`, else `Host`); a foreign origin is still 403 (checked in the container) |
| One onboarding browser check waited for the URL `**/dashboard` but the app ends on `/dashboard?welcome=1`, so it timed out on every run | The check accepts the query string; 46 of 46 pass |

## 2026-10-24: onboarding wizard (trading experience, brokers, plan, complete) with saved answers

### Features

| Feature | Where |
|---|---|
| `GET` / `PUT /api/v1/users/me/onboarding` and `POST /api/v1/users/me/onboarding/complete`. The user is the caller's token (gateway `X-User-Id`), never the URL or body. `PUT` saves only the fields sent (one step at a time, resumable); experience BEGINNER to PROFESSIONAL; 1 to 6 brokers (GROWW, ZERODHA, BINANCE, EXNESS, OTHER, NONE, de-duplicated, NONE exclusive); plan FREE to ENTERPRISE (an interest only, billing does not exist). `complete` needs all three answers and keeps its first time. | `user-service` `OnboardingService`, `UserController` |
| New nullable columns on `users`: `trading_experience`, `broker_preferences` (CSV), `selected_plan`, `onboarding_completed_at`. `UserResponse` gets `onboardingCompletedAt` (last). | `entity/User`, `dto/UserResponse` |
| Web app: `/onboarding` four-step wizard with a progress bar and "Step n of 4", focus moved to each step's heading, native radios and checkboxes (44 px), "I do not use a broker yet" excluding the others, a summary with Change links, resume at the first unanswered step, and "Skip for now" (this browser session only). Paid plans say plainly that they are not available yet and that everyone starts on Free. | `web-app` `components/onboarding/OnboardingWizard.tsx`, `/api/onboarding` routes |
| The dashboard sends users whose profile has no completion time to the wizard (every existing account, once). | `DashboardView` |
| `callAsUser` in `lib/auth/server.ts`: one place that calls user-service as the signed-in user, renewing the session from the refresh cookie when needed (the profile and onboarding routes share it). | `web-app` |

### Bugs found by testing and fixed

| Bug | Fix |
|---|---|
| After "Complete setup" the dashboard bounced straight back to the wizard: **each layout created its own data cache** and Next keeps visited pages in memory, so the "done" update never reached the dashboard's cache | One shared `QueryClient` in the browser (`components/Providers.tsx`) |
| Login and register seeded the profile cache with an answer that has no onboarding status, which the dashboard then trusted | They now clear the cached profile instead, so the dashboard fetches the full one |
| `completedAt` showed 7 fractional digits the first time and 6 afterwards (in-memory nanoseconds vs what MySQL stores) | Truncated to microseconds when set, with a unit test |
| `scripts/stop-all.ps1` left orphan JVMs: on this machine `java` is a launcher shim that starts the real JVM as a child, and only the shim's pid is recorded; the orphan kept the jar locked and broke the next build | Uses `taskkill /T /F` to stop the whole process tree |

### Compatibility

- Additive: new endpoints, nullable columns, one new response field. Nothing existing changed.
- **Every existing account has no completion time**, so each is offered the wizard the next time it reaches the dashboard (skippable per session).
- Selecting a paid plan records interest only: there is no billing, entitlement or price yet.

### Tests

user-service **76 tests pass** (10 new). Web app **46 of 46** browser checks on the mock gateway, three runs in a row (8 new for onboarding: step 1 start and missing-choice message, all four steps with focus and plan names, broker exclusivity, resume after reload, skip per session, finished user not sent back, session required and 44 px radios). **Real stack: 16 of 16** (`npm run e2e:auth:real`, now including the wizard writing to MySQL and a fresh browser going straight to the dashboard). Direct checks on the real service: all five bad values give 400, `complete` with nothing gives 400, partial saves keep earlier answers, brokers are sorted and de-duplicated, `complete` twice keeps one time, and a forged `X-User-Id` still returns the caller's own data. **Newman (Users & Auth folder, real stack): 24 requests, 41 assertions, 0 failures.** Postman 112 to 119 requests. The other Newman folders (market data, orders, portfolio, notifications, accounts, gateway security) were not run: their services are not started.

## 2026-10-24: authentication journey verified on the real local stack

Everything from the profile fields, refresh endpoint, password reset and login hardening entries below was previously tested only with mocks and H2. It was run end to end on the real local stack: web app, api-gateway, user-service, MySQL, Kafka and Mailpit in Docker.

| Checked | Result |
|---|---|
| Hibernate `update` on the existing MySQL database | Added the 7 nullable `users` columns and created `password_reset_tokens`; the 18 existing users were untouched |
| Register through the gateway with every profile field | Mobile, gender, country, terms version and a server-set acceptance time stored; passwords BCrypt |
| Refresh through the real gateway | A deleted access cookie is renewed from the refresh cookie |
| Wrong password vs unknown email | Both `401 Invalid Credentials` at the gateway and the same message in the web app |
| Forgot password | A real email arrived in Mailpit with a working link; none for an unregistered address; a second request inside 60 s sent nothing |
| Reset | Password changed; old password rejected, new one accepted; the same link refused the second time; the token stored only as a 64-character hash, all tokens used or voided |
| Session cut-off | A session started before the reset can no longer renew; one started after can |
| Blocked account (set to SUSPENDED in MySQL) | Correct password 403, wrong password 401, refresh 401; reactivated, login 200 |
| Kafka `user.registered` on the real broker | Keys `firstName, lastName, walletBalance, userId, email` only: no mobile, gender, country or terms fields |

New script `web-app/e2e/auth.real-stack.mjs` (`npm run e2e:auth:real`, 14 checks). It creates test users (`e2e-<timestamp>@example.com`) in the database it runs against; they were removed afterwards and the two registration events they produced remain in the Kafka topic. **Not done:** the Postman collection with Newman, the other five services, a run in the cluster.

## 2026-10-24: login hardening (no email enumeration, blocked accounts cannot log in)

### Fixes

| Problem | Fix | Where |
|---|---|---|
| Login with an unknown e-mail returned `404 User not found`, so anyone could find out which e-mails are registered | Unknown e-mail and wrong password both give `401 Invalid Credentials`. For an unknown e-mail a dummy BCrypt check runs, so the response time does not reveal it either. | `user-service` `UserServiceImpl.login` |
| Login ignored the user status: a SUSPENDED, BANNED or locked user could still log in | After a **correct** password, a blocking status (INACTIVE, SUSPENDED, DELETED, BLOCKED, BANNED, ACCOUNT_LOCKED/EXPIRED/DISABLED) gives `403 This account is not active`. A wrong password never reveals the status. | same, `AccountPolicy` |
| Login relied on matching the text of a generic `RuntimeException` ("not found", "invalid credentials") to pick a status | Typed `InvalidCredentialsException` (401) and `AccountNotActiveException` (403), mapped in `GlobalExceptionHandler` | `user-service` |
| Web app | A blocked account sees "This account is not active. Please contact support." only after the correct password; a wrong password still shows the generic message | `web-app` `lib/auth/server.ts`, `errors.ts` |

### Compatibility

- **Behaviour change for clients:** an unknown e-mail now returns 401, not 404. The web app and Postman already treated both as a failed login.
- Statuses such as PENDING_VERIFICATION and EMAIL_NOT_VERIFIED can still log in (email verification does not exist yet).
- Blocking an account does not end an access token already issued (at most 15 minutes); refresh tokens are refused as before.

### Tests

user-service **66 tests pass** (5 new: same exception and message for unknown e-mail and wrong password with the dummy BCrypt check verified, six blocking statuses refused only after the correct password, ACTIVE / legacy null / pending-verification still log in, controller 401 and 403). Web app **39 of 39** browser checks (1 new, mock gateway updated to the real behaviour). Postman 111 → 112 requests (wrong-password test tightened to exactly 401, unknown-email test added). Not run on MySQL, through the real gateway or with Newman. Docs: user-service README, technical documentation (gaps), user manual, root `CLAUDE.md` gaps, web-app README, plan.

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
