# CLAUDE.md: portfolio-service

Read `README.md` in this folder first.

## Rules

- Holdings change only through the Kafka consumer (`kafka/PortfolioEventConsumer`); the REST API is read-only.
- The consumer reads `order.executed` as a `Map` with keys `userId, symbol, type, quantity, price`; keep it tolerant of extra fields.
- `Holding` has hand-written getters/setters (no Lombok); follow that style in this class or convert the whole class.
- Kafka consumer config must stay `ErrorHandlingDeserializer` + `JacksonJsonDeserializer` with `spring.json.use.type.headers: false` (producers on Boot 3 and Boot 4 send different type headers).

## Gotchas

- `PortfolioEventConsumer` catches every exception: errors only appear in the log. When adding logic, prefer letting exceptions propagate and configuring a `DefaultErrorHandler` (see notification-service `config/KafkaConsumerConfig`).
- `GET /api/v1/portfolio/{userId}` is not protected. Do not build features on it from outside; fix it first (recipe below).
- The `contextLoads` test needs docker-compose infrastructure.

## Recipes

**Protect `/{userId}`**
- Option A: in `PortfolioController`, accept `@RequestHeader("X-User-Id")` and `@RequestHeader(value="X-User-Role", required=false)`; allow when equal or ADMIN, else 403.
- Option B: add `/api/v1/portfolio/*` (excluding `/me`) to the gateway's `INTERNAL_PATHS` and call it only service-to-service.

**Retry failed updates**: remove the try/catch in the listener, add a `DefaultErrorHandler` bean with `FixedBackOff` and a `DeadLetterPublishingRecoverer`.

## Verify

```powershell
.\mvnw.cmd test
```
Postman folder 4 after folder 3 (holding = BUY 5 − SELL 2 = 3).
