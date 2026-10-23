# api-gateway

Single entry point: validates JWTs, forwards the caller's identity, routes `/api/v1/**` to the services.

| | |
|---|---|
| Port | `API_GATEWAY_PORT` (8080) |
| Base package | `com.stockmarket.apigateway` |
| Stack | Boot 4.0.8, **Spring Cloud Gateway server-webmvc** (servlet, *not* WebFlux), Spring Cloud 2025.1.3, jjwt 0.12.6 |
| Database | none |

## Run and test

```powershell
.\mvnw.cmd spring-boot:run        # needs JWT_SECRET from ../.env
.\mvnw.cmd test                   # 9 tests (JwtAuthFilterTest + contextLoads), no infrastructure
```

## Routes (`spring.cloud.gateway.server.webmvc.routes`)

| Route | Paths | Target |
|---|---|---|
| user-service | `/api/v1/users/**` | `${hosts.user-service}:${USER_SERVICE_PORT}` |
| market-data-service | `/api/v1/market-data/**` | `…:${MARKET_DATA_SERVICE_PORT}` |
| order-service | `/api/v1/orders/**` | `…:${ORDER_SERVICE_PORT}` |
| portfolio-service | `/api/v1/portfolio/**` | `…:${PORTFOLIO_SERVICE_PORT}` |
| notification-service | `/api/v1/notifications/**` | `…:${NOTIFICATION_SERVICE_PORT}` |
| account-service | `/api/v1/accounts/**`, `/trades/**`, `/holdings/**`, `/fees/**`, `/charges/**`, `/taxes/**`, `/dividends/**`, `/interest/**`, `/statements/**` | `…:${ACCOUNT_SERVICE_PORT}` |

## `JwtAuthFilter` (servlet `OncePerRequestFilter`)

Order of checks for every request whose path starts with `/api/`:

1. Skip `OPTIONS` and `PUBLIC_PATHS`: `/api/v1/users/register`, `/api/v1/users/login`, `/api/v1/users/refresh`, `/api/v1/users/forgot-password`, `/api/v1/users/reset-password`, `/api/v1/market-data/**`.
2. `INTERNAL_PATHS` → 403 always: `/api/v1/users/*/funds/deduct`, `/funds/credit`.
3. `Authorization: Bearer …` missing → 401.
4. Verify HS256 signature (`jwt.secret`), expiry, issuer (`jwt.issuer`), `tokenType=ACCESS` → otherwise 401 (JSON body).
5. Role = claim `role` (USER if absent). `ADMIN_PATHS` (`/api/v1/accounts/*/dividends`, `/interest`) → 403 unless ADMIN.
6. Wrap the request: drop **every** client `X-User-*` header; add `X-User-Id` (claim `userId`, else `sub`), `X-User-Email`, `X-User-Role`.

Path patterns use Spring `AntPathMatcher`.

## Configuration

| Key | Notes |
|---|---|
| `hosts.*` | one per service (localhost; `docker` profile → container names) |
| `jwt.secret`, `jwt.issuer` | `JWT_SECRET` (no default), `JWT_ISSUER`; must match user-service |
| unused starters | reactive Redis, Kafka (reserved for rate limiting / audit) |

## Known limitations

- No rate limiting, no CORS configuration, no request logging/correlation id.
- Services trust the forwarded headers: their ports must not be reachable except through the gateway.
