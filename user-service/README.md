# user-service

Accounts of people: registration, login, JWT issuing, profile, role and the wallet balance used by order-service.

| | |
|---|---|
| Port | `USER_SERVICE_PORT` (8081) |
| Base package | `com.stockmarket.userservice` |
| Spring Boot | **3.5.6** (the only Boot 3 service: Jackson 2, `spring-boot-starter-web`, Kafka `JsonSerializer`) |
| Database | MySQL, table `users`, Hibernate `ddl-auto: update` (no Flyway) |
| Publishes | Kafka `user.registered` |
| Called by | api-gateway (all routes), order-service (`funds/deduct`, `funds/credit`), account-service (`/me`, `/{id}`) |

## Run and test

```powershell
.\mvnw.cmd spring-boot:run        # from user-service/, needs MySQL + Kafka (docker compose up -d)
.\mvnw.cmd test                   # 33 tests, no infrastructure (H2, mocks)
```

## Package layout

```
com.stockmarket.userservice
├── UserServiceApplication
├── config/SecurityConfig          BCrypt bean; CSRF off; stateless; permits all (gateway does auth)
├── controller/UserController      /api/v1/users endpoints + owner check (requireOwner)
├── dto/                           RegisterRequest, LoginRequest, AuthResponse, UserResponse (incl. role)
├── entity/                        User, UserStatus (ACTIVE, SUSPENDED, ... 15 values), Role (USER, ADMIN)
├── exception/                     GlobalExceptionHandler, UserAccessDeniedException
├── repository/UserRepository      findByEmail, existsByEmail
├── security/JwtService            issue/parse access + refresh tokens (HS256)
└── service/UserService, impl/UserServiceImpl
```

## API (`/api/v1/users`)

| Method | Path | Auth | Behaviour |
|---|---|---|---|
| POST | `/register` | public | Create user (BCrypt), wallet = `initialBalance` (default 10000), publish `user.registered`, return tokens (201). Duplicate e-mail → 400. Optional profile fields below |
| POST | `/login` | public | Tokens. Unknown e-mail → 404, wrong password → 401 |
| POST | `/refresh` | public | Body `{refreshToken}`. Returns a new access token and the same refresh token (not rotated). Bad, expired, tampered or access token, unknown user, or a blocked status (INACTIVE, SUSPENDED, DELETED, BLOCKED, BANNED, ACCOUNT_LOCKED/EXPIRED/DISABLED) → 401 `Invalid refresh token`. Public because the access token has already expired; the gateway rejects refresh tokens on every other path |
| GET | `/me` | X-User-Id | Own profile |
| GET | `/{userId}` | X-User-Id (+ X-User-Role) | Own profile, or any profile for ADMIN; else 403 |
| POST | `/{userId}/funds/add?amount=` | X-User-Id | Owner only; amount > 0 |
| POST | `/{userId}/funds/deduct?amount=` | none | **Internal** (order-service); gateway blocks it. Insufficient → 400 |
| POST | `/{userId}/funds/credit?amount=` | none | **Internal** (order-service); gateway blocks it |

**Optional register fields** (the web app makes them mandatory; the API keeps them optional so older clients still work). Validated when present:

| Field | Rule |
|---|---|
| `mobileCountryCode` | `+` and 1–4 digits, e.g. `+91`. Must be sent together with `mobileNumber` |
| `mobileNumber` | 6–14 digits, national number only. Stored as entered, **not verified** (no OTP) |
| `gender` | `MALE` or `FEMALE` |
| `country` | 2-letter uppercase ISO code, e.g. `IN` |
| `termsVersion` | Version of the Terms and Risk Disclaimer accepted (max 50 characters). The server sets `termsAcceptedAt` itself; a client-sent time is never used |

Responses: `AuthResponse {userId, email, firstName, lastName, walletBalance, accessToken, refreshToken, tokenType}`, `UserResponse {id, email, firstName, lastName, walletBalance, status, createdAt, role, mobileCountryCode, mobileNumber, gender, country, termsVersion, termsAcceptedAt}` (the new fields are `null` for users who registered before they existed).

## Data model: `users`

| Column | Notes |
|---|---|
| id | UUID string |
| email | unique |
| password | BCrypt hash |
| first_name, last_name | |
| wallet_balance | DECIMAL(15,2) |
| status | `UserStatus`, default ACTIVE |
| role | `varchar(20) not null default 'USER'` (`USER` / `ADMIN`) |
| created_at | |
| mobile_country_code, mobile_number, gender, country | nullable profile fields (`varchar`; `gender` is `MALE`/`FEMALE`); existing users have none |
| terms_version, terms_accepted_at | nullable; set together at registration, time from the server clock |

## Tokens (`JwtService`)

| Token | Lifetime | Claims |
|---|---|---|
| Access | `jwt.access-token-expiry-minutes` (15) | `sub`=userId, `userId`, `email`, `role`, `tokenType=ACCESS`, `iss` |
| Refresh | `jwt.refresh-token-expiry-days` (7) | `sub`, `userId`, `tokenType=REFRESH`; exchanged at `POST /refresh` |

`generateAccessToken(userId, email)` defaults the role to USER; `generateAccessToken(userId, email, role)` is what register/login use. `extractRole` returns USER for tokens without the claim.

## Events

`user.registered`, key userId: `{userId, email, firstName, lastName, walletBalance}`. Consumed by notification-service. The mobile number, gender, country and terms fields are deliberately **not** published (personal data nobody downstream needs); a test pins this contract.

## Configuration

| Key | Source |
|---|---|
| `server.port` | `USER_SERVICE_PORT` |
| `hosts.mysql`, `hosts.kafka` | localhost by default; `docker` profile → `.env` hosts |
| `spring.datasource.*` | `MYSQL_*` from `.env` |
| `jwt.secret`, `jwt.issuer` | `JWT_SECRET` (no default; must equal the gateway's), `JWT_ISSUER` |
| tests | `src/test/resources/application.yaml` **replaces** the main file entirely (H2, fixed secret) |

## Known limitations

- Login with an unknown e-mail returns 404 (reveals registered e-mails); should be 401.
- Refresh tokens are stateless: they are not rotated and cannot be revoked, so a stolen one works until it expires (7 days) or the user's status blocks it. There is no logout endpoint (the web app only clears its cookies).
- Login does not check `status`, so a SUSPENDED user can still log in; only `/refresh` refuses blocked statuses.
- No rate limiting on `/login` or `/refresh`.
- `GlobalExceptionHandler` maps generic `RuntimeException`s by message text ("not found" → 404, "invalid credentials" → 401); prefer typed exceptions.
- `JWT_EXPIRATION` in `.env` is not read (`jwt.access-token-expiry-minutes` is).
