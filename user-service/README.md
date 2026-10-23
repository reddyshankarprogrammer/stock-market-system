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
.\mvnw.cmd test                   # 76 tests, no infrastructure (H2, mocks, an in-process SMTP server)
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
├── service/PasswordResetService   forgot and reset password (hashed single-use tokens)
├── service/OnboardingService      the answers of the onboarding wizard (saved step by step)
├── service/MailService, impl/SmtpMailService   outgoing email; failures are logged, never thrown
└── service/UserService, impl/UserServiceImpl
```

## API (`/api/v1/users`)

| Method | Path | Auth | Behaviour |
|---|---|---|---|
| POST | `/register` | public | Create user (BCrypt), wallet = `initialBalance` (default 10000), publish `user.registered`, return tokens (201). Duplicate e-mail → 400. Optional profile fields below |
| POST | `/login` | public | Tokens. An unknown e-mail and a wrong password both give the same `401 Invalid Credentials` (and the same response time: a dummy BCrypt check runs for an unknown e-mail). Correct password but a blocking status (INACTIVE, SUSPENDED, DELETED, BLOCKED, BANNED, ACCOUNT_LOCKED/EXPIRED/DISABLED) → `403 This account is not active`; the status is only revealed after the correct password |
| POST | `/refresh` | public | Body `{refreshToken}`. Returns a new access token and the same refresh token (not rotated). Bad, expired, tampered or access token, unknown user, or a blocked status (INACTIVE, SUSPENDED, DELETED, BLOCKED, BANNED, ACCOUNT_LOCKED/EXPIRED/DISABLED) → 401 `Invalid refresh token`. Public because the access token has already expired; the gateway rejects refresh tokens on every other path |
| POST | `/forgot-password` | public | Body `{email}`. **Always 202** with the same message, whether or not the email is registered. If it is (and the account is not blocked), a single-use link is emailed. Asking again within 60 s sends nothing new; asking later voids the older link |
| POST | `/reset-password` | public | Body `{token, newPassword}` (6–72 characters). 200 and the new password works; the token is spent and any other pending links are voided. Unknown, used, expired or blocked-account token → 400 `This reset link is invalid or has expired` (one message for all) |
| GET | `/me/onboarding` | X-User-Id | Own onboarding answers: `{tradingExperience, brokerPreferences[], plan, completed, completedAt}` (all empty for a new user). The user id is the gateway header, never the URL or body |
| PUT | `/me/onboarding` | X-User-Id | Save one step or several; **only the fields sent change**, so a half-finished onboarding can be resumed. `tradingExperience` BEGINNER, INTERMEDIATE, ADVANCED or PROFESSIONAL; `brokerPreferences` (1 to 6 of GROWW, ZERODHA, BINANCE, EXNESS, OTHER, NONE; duplicates removed; NONE cannot be combined); `plan` FREE, PRO, AI_PRO, QUANT_PRO or ENTERPRISE (an interest only: billing does not exist yet). Bad values → 400 |
| POST | `/me/onboarding/complete` | X-User-Id | Marks onboarding done (needs experience, brokers and plan saved first, else 400). Completing again keeps the first time. `UserResponse.onboardingCompletedAt` then becomes non-null |
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

Responses: `AuthResponse {userId, email, firstName, lastName, walletBalance, accessToken, refreshToken, tokenType}`, `UserResponse {id, email, firstName, lastName, walletBalance, status, createdAt, role, mobileCountryCode, mobileNumber, gender, country, termsVersion, termsAcceptedAt, onboardingCompletedAt}` (the new fields are `null` for users who registered before they existed).

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
| password_changed_at | nullable; set by a password reset. Refresh tokens issued before it are refused |
| trading_experience, broker_preferences, selected_plan | nullable onboarding answers (`broker_preferences` is comma-separated, sorted, e.g. `GROWW,ZERODHA`); existing users have none |
| onboarding_completed_at | nullable; set once, at microsecond precision (what MySQL stores) |

### Table `password_reset_tokens`

| Column | Notes |
|---|---|
| id | UUID string |
| user_id | the user the link belongs to |
| token_hash | SHA-256 of the token, unique. **The token itself is never stored**, so a database leak gives no working links |
| expires_at | 30 minutes after creation (`app.password-reset.expiry-minutes`) |
| used_at | set when the token is used or voided; null while it can still be used |
| created_at | |

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
| `hosts.mail`, `spring.mail.port` | localhost / `MAIL_PORT` (1025) by default (Mailpit from docker compose); `docker` profile: `MAIL_HOST` (default `mailpit`) |
| `app.base-url` | `APP_BASE_URL` (default `http://localhost:3000`): where the reset link in the email points |
| `app.mail-from` | `MAIL_FROM` |
| `app.password-reset.expiry-minutes`, `.cooldown-seconds` | `PASSWORD_RESET_EXPIRY_MINUTES` (30), `PASSWORD_RESET_COOLDOWN_SECONDS` (60) |
| tests | `src/test/resources/application.yaml` **replaces** the main file entirely (H2, fixed secret) |

## Known limitations

- Refresh tokens are stateless: they are not rotated and cannot be revoked, so a stolen one works until it expires (7 days) or the user's status blocks it. There is no logout endpoint (the web app only clears its cookies).
- The blocking statuses are a fixed list in `AccountPolicy`; statuses such as PENDING_VERIFICATION and EMAIL_NOT_VERIFIED still log in (email verification does not exist yet). Blocking an account does not end an access token already issued (at most 15 minutes).
- No rate limiting on `/login`, `/refresh` or `/reset-password` (`/forgot-password` has only the 60-second per-user cooldown).
- A password reset ends refresh tokens issued before it, but an access token already issued keeps working until it expires (at most 15 minutes).
- No email provider is chosen for production: until `MAIL_HOST` is set there, reset emails fail and are logged (the API answer is unchanged). Expired reset tokens are never deleted.
- Reset links are single-use but the token is in the URL: the web app removes it from the address bar and sends no referrer.
- `GlobalExceptionHandler` maps generic `RuntimeException`s by message text ("not found" → 404, "invalid credentials" → 401); prefer typed exceptions.
- `JWT_EXPIRATION` in `.env` is not read (`jwt.access-token-expiry-minutes` is).
