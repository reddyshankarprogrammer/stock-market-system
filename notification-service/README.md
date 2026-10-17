# notification-service

Turns user and order events into per-user notifications.

| | |
|---|---|
| Port | `NOTIFICATION_SERVICE_PORT` (8085) |
| Base package | `com.stockmarket.notificationservice` |
| Spring Boot | 4.0.8 |
| Storage | **In memory** (no database), last `notification.max-per-user` (100) per user |
| Consumes | Kafka `user.registered`, `order.executed`, `order.failed` (group `notification-service-group`) |

## Run and test

```powershell
.\mvnw.cmd spring-boot:run        # needs Kafka
.\mvnw.cmd test                   # 6 tests; listener test needs nothing, contextLoads starts without a broker
```

## Package layout

```
com.stockmarket.notificationservice
├── NotificationServiceApplication
├── config/KafkaConsumerConfig            DefaultErrorHandler: 2 retries 1 s apart, then log and skip;
│                                         IllegalArgumentException is not retried
├── controller/NotificationController     GET /api/v1/notifications
├── listener/NotificationEventListener    one @KafkaListener per topic; field injection (@Autowired)
├── model/Notification (record), NotificationType (WELCOME, ORDER_EXECUTED, ORDER_FAILED)
└── service/NotificationService           send (store + log), getNotifications (newest first)
```

## Events handled

| Topic | Required fields | Notification |
|---|---|---|
| `user.registered` | userId, firstName, walletBalance | WELCOME: "Welcome {firstName}! AED {balance} has been added to your wallet." |
| `order.executed` | userId, type, symbol, quantity, price, totalAmount | ORDER_EXECUTED |
| `order.failed` | userId, type, symbol, quantity (+ reason) | ORDER_FAILED |

A missing field throws `IllegalArgumentException` → the record is skipped without retry.

## API

| Method | Path | Behaviour |
|---|---|---|
| GET | `/api/v1/notifications` | Caller's (`X-User-Id`) notifications, newest first |

## Configuration

| Key | Notes |
|---|---|
| `hosts.kafka` | localhost:9092; `docker` profile → `KAFKA_BOOTSTRAP_SERVERS` |
| `kafka.topics.*` | user-registered, order-executed, order-failed |
| `notification.max-per-user` | cap per user |
| consumer | `ErrorHandlingDeserializer` → `JacksonJsonDeserializer` into `HashMap`, type headers ignored |

## Known limitations

- Lost on restart (in memory); delivery is log-only (no push / e-mail / SMS).
- Amounts are labelled "AED" while stock prices are USD.
- account-service events (`account.trade.executed`, `account.fund.transaction`) are not consumed.
