# CLAUDE.md: order-service

Read `README.md` in this folder first.

## Rules

- Keep `placeOrder` free of `@Transactional`: it calls two remote services. If you need atomicity, use an outbox or compensation, not a long DB transaction.
- The caller is `X-User-Id`; the controller overwrites any `userId` in the body. Never trust a body user id.
- Throw the typed exceptions in `exception/` and map new ones in `GlobalExceptionHandler`; Feign errors are mapped there too (404 → 404, others → 502).
- Event payloads (`order.executed`, `order.failed`) are consumed by portfolio-service and notification-service as maps: only **add** fields; renaming `type`, `userId`, `symbol`, `quantity`, `price`, `totalAmount` breaks consumers.
- Topic names come from `kafka.topics.*`; tests set them with `ReflectionTestUtils`.

## Gotchas

- `OrderServiceTest` injects topic names via reflection (`@Value` fields): add new `@Value` fields there too.
- `Order.executedAt` must stay without `@CreationTimestamp` (it is set only on execution).
- `UserServiceClient` endpoints are blocked at the gateway; they are reachable only service-to-service.

## Recipes

**Check holdings before a SELL**
1. Add a Feign client to portfolio-service (`GET /api/v1/portfolio/{userId}` is internal) or a dedicated holdings endpoint there.
2. In `placeOrder`, before `creditFunds`, reject with a 400 exception when quantity > held.
3. Test in `OrderServiceTest` with the client mocked.

**Add a field to orders**: entity → (Hibernate adds the column) → event map in `publishOrderEvent` if consumers need it → README.

## Verify

```powershell
.\mvnw.cmd test
```
Postman folder 3 (BUY, SELL, insufficient funds 422, unknown symbol 404).
