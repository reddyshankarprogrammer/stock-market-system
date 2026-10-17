# CLAUDE.md: api-gateway

Read `README.md` in this folder first.

## Rules

- This is **server-webmvc** (servlet). Do not use WebFlux types (`ServerWebExchange`, `Mono`, `GatewayFilter`, `AbstractGatewayFilterFactory`); filters are servlet filters.
- Routes live under `spring.cloud.gateway.server.webmvc.routes` (not `spring.cloud.gateway.routes`). A route may list several paths: `Path=/a/**,/b/**`.
- Security-relevant path lists are constants in `JwtAuthFilter`: `PUBLIC_PATHS`, `INTERNAL_PATHS`, `ADMIN_PATHS`. Every change to them needs a test in `JwtAuthFilterTest`.
- Claim names and `JWT_SECRET`/`JWT_ISSUER` are shared with user-service `security/JwtService`.
- Keep stripping all client-sent `X-User-*` headers: downstream services trust them.

## Gotchas

- On Windows a running gateway locks `target/api-gateway-0.0.1-SNAPSHOT.jar`: stop it before `mvnw package` (the build fails at repackage otherwise).
- `JwtAuthFilter` only filters paths starting with `/api/`; actuator is never filtered.
- `IdentityHeadersRequest` overrides `getHeader`, `getHeaders` and `getHeaderNames`; the proxy reads headers through these, so keep all three consistent.

## Recipes

**Route a new service**
1. Add `hosts.<service>: localhost` (+ docker profile value) and a route with its paths in `application.yaml`.
2. Add `<SERVICE>_PORT` to `.env` if new.
3. Restart the gateway; check with a request through `localhost:8080`.

**Make an endpoint public / internal / admin-only**: add the pattern to the matching list in `JwtAuthFilter`, add a test, and update `docs/technical-documentation.md` section 9.

**Forward a new identity claim**: read it from `claims` and `identity.put("X-User-…", value)`; test that a spoofed client header is replaced.

## Verify

```powershell
.\mvnw.cmd test
```
Postman folder 6 (missing/garbage/refresh tokens, spoofed header, internal endpoint).
