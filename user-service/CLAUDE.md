# CLAUDE.md: user-service

Read `README.md` in this folder first (endpoints, data model, tokens, config).

## Rules

- **Spring Boot 3.5.6, not 4.** Use `spring-boot-starter-web` APIs, Jackson 2 (`com.fasterxml.jackson.databind`), Kafka `JsonSerializer`. Do not copy Boot 4 code (Jackson 3 `tools.jackson.*`, `JacksonJsonSerializer`, `spring-boot-starter-webmvc`) from other services.
- Identity comes from `X-User-Id` (and `X-User-Role`) set by the gateway; never from a request body.
- Every endpoint that takes a `{userId}` must call `requireOwner` unless it is internal (then add it to the gateway's `INTERNAL_PATHS`).
- The JWT claim names (`userId`, `email`, `role`, `tokenType`) and `JWT_SECRET` / `JWT_ISSUER` are a contract with `api-gateway/.../JwtAuthFilter`; change both together.
- Schema is managed by Hibernate (`ddl-auto: update`). New NOT NULL columns need a `columnDefinition` with a default (see `User.role`), or existing rows break.
- New business errors: throw a specific exception and map it in `GlobalExceptionHandler`; do not rely on the message-text mapping of `RuntimeException`.

## Gotchas

- `SecurityConfig` permits everything; authorization is the gateway's job plus `requireOwner`. Do not add Spring Security auth here without also changing the gateway model.
- Tests use `src/test/resources/application.yaml`, which fully replaces the main YAML: new required properties must be added there too, or the context test fails.
- `UserServiceImplTest` stubs `jwtService.generateAccessToken(id, email, "USER")`: keep the 3-argument call in sync.
- `UserResponse` uses `@AllArgsConstructor`; adding a field changes the constructor used in `UserControllerTest`.

## Recipes

**Add a field to the user profile**
1. Add the field to `entity/User` (with a DB default if NOT NULL).
2. Add it to `dto/UserResponse` (last position) and `UserServiceImpl.mapToUserResponse`.
3. Update the `new UserResponse(...)` calls in `UserControllerTest`.
4. If other services read it (account-service `client/UserDto`), add it there; unknown fields are ignored, so it is optional.

**Add a claim to the access token**
1. `JwtService.generateAccessToken` + a constant + an `extract…` method; test in `JwtServiceTest`.
2. If downstream services need it, forward it in the gateway's `JwtAuthFilter` as an `X-User-*` header.

**Add an internal (service-to-service) endpoint**
1. No ownership check; document it as internal in the controller.
2. Add its path to `INTERNAL_PATHS` in `api-gateway/src/main/java/com/stockmarket/apigateway/filter/JwtAuthFilter.java` and a test in `JwtAuthFilterTest`.

## Verify

```powershell
.\mvnw.cmd test
```
Then through the gateway: `POST /api/v1/users/register`, `GET /api/v1/users/me` with the token (Postman folder 1).
