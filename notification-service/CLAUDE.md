# CLAUDE.md: notification-service

Read `README.md` in this folder first.

## Rules

- One listener method per topic in `NotificationEventListener`; topic names from `kafka.topics.*`; the consumer group comes from `spring.kafka.consumer.group-id` (do not set `groupId` on the annotation).
- Validate required event fields with the `required(...)` helper so bad events become `IllegalArgumentException` (skipped, not retried).
- Payloads arrive as `Map<String, Object>`; never bind to producer classes (producers run Boot 3 and Boot 4 with different type headers).
- The listener uses `@Autowired` field injection (the owner's choice); keep it and set the field in tests with `ReflectionTestUtils.setField(listener, "notificationService", ...)`.

## Gotchas

- `NotificationService` keeps data in a `ConcurrentHashMap` of `ConcurrentLinkedDeque`s; anything persistent needs a database (add starters, entity, Flyway) first.
- `NotificationController` uses field injection together with `@RequiredArgsConstructor` (no final fields, so Lombok generates nothing); fine as is, do not mix in final fields without removing `@Autowired`.

## Recipes

**Consume a new topic** (e.g. `account.trade.executed`)
1. Add `kafka.topics.<name>` to `application.yaml`.
2. Add a `@KafkaListener(topics = "${kafka.topics.<name>}")` method that reads the map with `required(...)` and calls `notificationService.send(...)`.
3. Add a `NotificationType` value.
4. Add a test in `NotificationEventListenerTest`.

**Persist notifications**: add `spring-boot-starter-data-jpa` + MySQL, a `Notification` entity and repository, and replace the in-memory map inside `NotificationService` (keep its method signatures).

## Verify

```powershell
.\mvnw.cmd test
```
Postman folder 5 (WELCOME, ORDER_EXECUTED, ORDER_FAILED present).
