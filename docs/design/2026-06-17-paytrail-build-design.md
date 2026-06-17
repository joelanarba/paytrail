# PayTrail — Build Design

Date: 2026-06-17
Status: Approved

This document is the authoritative design for the initial build of PayTrail.

## 1. Overview

PayTrail is a standalone Spring Boot service that ingests Paystack webhook events,
persists each event immediately, then processes them asynchronously with idempotency
guarantees, retries, and a dead-letter path. It separates ingestion from processing so
the webhook returns `200` in under 200ms regardless of downstream work, avoiding the
duplicate-processing problems caused by inline synchronous webhook handling and Paystack
retries.

A query API (API-key protected, merchant-scoped) exposes event history, payment status,
per-merchant summaries, and dead-letter management.

## 2. Architecture

```
Paystack
  -> POST /api/v1/webhooks/paystack   (public)
       1. verify HMAC-SHA512 signature
       2. parse event type + reference + merchantId (default "UNKNOWN")
       3. store WebhookEvent { status: RECEIVED, eventId: UUID }
       4. return 200 { eventId }   (no processing inline)

Scheduler (every SCHEDULER_INTERVAL_MS, default 10000ms)
  -> fetch up to SCHEDULER_BATCH_SIZE (default 50) events with status RECEIVED
  -> submit each to EventProcessorService on an @Async pool (10 fixed threads)

EventProcessorService.processEvent(event):
  1. acquire Redis lock  lock:event:{eventId}  (SET NX EX 30); skip if not acquired
  2. idempotency check   idempotency:{reference}:{paystackEvent}; if PROCESSED -> mark PROCESSED, release, return
  3. set status PROCESSING
  4. route by paystackEvent to a handler (unknown type -> log INFO, mark PROCESSED, set idempotency key)
  5. on success -> status PROCESSED + processedAt; set idempotency key (TTL 7 days); release lock
  6. on failure -> retryCount++; if >= 3 -> DEAD_LETTER + failureReason; else -> RECEIVED; log stack trace; release lock

Query API (X-Api-Key, merchant-scoped; SUPER_API_KEY bypasses scoping)
  GET  /api/v1/events                      list with filters + pagination
  GET  /api/v1/events/{eventId}            single event (full parsedData)
  GET  /api/v1/payments/{reference}        payment status
  GET  /api/v1/payments                    list with filters + pagination
  GET  /api/v1/merchants/summary           current merchant summary
  GET  /api/v1/merchants/{id}/summary      super key only
  GET  /api/v1/dead-letters                list dead letters
  POST /api/v1/dead-letters/{eventId}/retry  re-queue (status RECEIVED, retryCount 0)
```

## 3. Key Technical Decisions

| Area | Decision | Rationale |
|---|---|---|
| Auth implementation | Plain `OncePerRequestFilter` (`ApiKeyAuthFilter`) registered via `FilterRegistrationBean`. No Spring Security on the classpath. | Simpler, fewer dependencies, and a good fit for a service-to-service API-key model. `SecurityConfig` only registers the filter; there is no CSRF/form-login to configure. |
| Super key | Recognized inside `ApiKeyAuthFilter`: if `X-Api-Key` equals `SUPER_API_KEY`, set super context (`isSuperKey = true`), which bypasses merchant scoping. | One auth path, no separate mechanism. |
| Auth scope | Filter applies to all routes except `/api/v1/webhooks/**` and `/api/v1/dev/**`. The dev seed endpoint validates `X-Super-Key` against `SUPER_API_KEY` itself. | Webhook is HMAC-secured; dev endpoint is super-key-gated by its own check. |
| Auth failure response | Filter writes a `401` `ApiResponse` error JSON directly to the response. | Consistent error shape across all failures. |
| Redis in tests | Mocked `RedisTemplate` beans via a test configuration; lock/idempotency asserted via interactions. Real Lettuce client at runtime. | Hermetic, fast, and avoids an external Redis or container dependency for the suite. |
| HMAC raw body | Controller takes `@RequestBody String rawBody`, verifies HMAC on the exact string, then parses with `ObjectMapper`. | Simpler and more robust than a custom `HttpMessageConverter` / content-caching wrapper. |
| Redis lock primitive | `redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(30))` returning `Boolean`. | Native `SET NX EX` semantics. |
| Document IDs | `@Id` with `String` type. | Simpler to serialize in the query API than a raw `ObjectId`. |
| Mongo indexes | Created on startup in `MongoConfig` via `IndexOperations`, with an INFO log confirming each. | Verifiable on boot; no reliance on annotation auto-index. |
| Timestamps | `java.time.Instant`, UTC. | Unambiguous storage and comparison. |
| Repository setup | Initialize git; ignore environment files, build output, IDE metadata, and local working notes. Conventional commit messages. | Clean history from the first commit. |

## 4. Engineering Conventions

1. No Lombok — explicit constructors, getters, setters.
2. No MapStruct — manual DTO mapping in the service layer.
3. Constructor injection only — never `@Autowired` on a field.
4. SLF4J for all logging; no `System.out.println` (the Sprint 4 startup key seed log is the single labelled exception).
5. No hardcoded secrets — all sensitive values from environment variables.
6. All endpoints wrapped in `ResponseEntity<ApiResponse<T>>`.
7. All timestamps stored as UTC `java.time.Instant`.
8. MongoDB document IDs use `@Id` with `String` type.
9. Every sprint ends with `mvn clean test` green and no compiler warnings before proceeding.

`ApiResponse<T>` success shape: `{ success: true, message: string, data: T }`.
Error shape: `{ success: false, message: string, errors: [...] }`.

## 5. Data Model (MongoDB)

- **`webhook_events`** — immutable raw events. Status enum:
  `RECEIVED | PROCESSING | PROCESSED | FAILED | DEAD_LETTER`. Core fields: `eventId` (UUID),
  `paystackEvent`, `reference`, `merchantId`, `rawPayload`, `parsedData`, `status`, `retryCount`,
  `failureReason`, `receivedAt`, `processedAt`, timestamps. Indexes: compound
  `(reference, paystackEvent)`, `status`, `merchantId`.
- **`payment_projections`** — current payment state. Status: `PENDING | SUCCESS | FAILED | REFUNDED`.
  Amounts in kobo (`Long`). Indexes: unique `reference`, `merchantId`.
- **`merchant_summaries`** — per-merchant aggregates (transaction counts, revenue, refunds,
  last transaction time). Index: unique `merchantId`.
- **`api_keys`** — `keyHash` (SHA-256, unique index), `merchantId`, `description`, `isActive`,
  `createdAt`, `lastUsedAt`.

Upserts in handlers use `MongoTemplate.upsert(query, update, Class)` (not repository `save`),
always setting `updatedAt`.

## 6. Sprint Breakdown (single plan, executed in order)

### Sprint 1 — Scaffold, MongoDB, API key auth, webhook ingestion
- Maven project: Spring Boot 3.x, Java 21. Dependencies: web, data-mongodb, data-redis,
  validation, embedded MongoDB (test scope). `application.properties` env-driven;
  `application-test.properties` uses embedded Mongo + mocked Redis.
- Document classes: `WebhookEvent`, `PaymentProjection`, `MerchantSummary`, `ApiKey`.
- `MongoConfig` creates indexes on startup with log confirmation.
- Repositories with finder methods (by status, reference+event, status+retryCount, merchantId, etc.).
- `ApiKeyService` (generate/validate, SHA-256 hash, returns raw key once, updates `lastUsedAt`).
- `ApiKeyAuthFilter` + `MerchantContext` thread-local (merchantId + isSuperKey, cleared after request).
- `SecurityConfig` registers the filter; excludes `/webhooks/**` and `/dev/**`.
- `HmacUtil` (`computeHmacSha512` lowercase hex; `verifySignature` constant-time via `MessageDigest.isEqual`).
- `WebhookController` + `WebhookIngestionService`: verify -> parse -> store RECEIVED -> 200, no inline processing.
- Dev seed endpoint `POST /api/v1/dev/api-keys` gated by `X-Super-Key`.
- `GlobalExceptionHandler` (401/404/400/409/validation/500).
- Tests: `HmacUtilTest`, `ApiKeyServiceTest`, `ApiKeyAuthFilterTest` (MockMvc), `WebhookControllerTest`.
- Gate + commit: `feat: scaffold project, MongoDB setup, API key auth, webhook ingestion`.

### Sprint 2 — Processor, idempotency, dead letters
- `SchedulerConfig` (`@EnableScheduling`), `AsyncConfig` (fixed pool of 10).
- `EventProcessorScheduler` (every 10s, batch 50, submit via `@Async`).
- `EventProcessorService` (lock, idempotency, PROCESSING, route, success/failure handling).
- Handlers: `ChargeSuccessHandler`, `ChargeFailedHandler`, `RefundHandler`,
  `TransferSuccessHandler`, `TransferFailedHandler` (transfers log only, no projection).
- Upsert pattern via `MongoTemplate`.
- Tests: `EventProcessorServiceTest` (happy path, lock contention, idempotency no-op,
  retry increment, dead letter at 3), handler tests (projection + summary correctness, DB-level idempotency).
- Gate + commit: `feat: event processor, idempotency, retry logic, dead letter handling`.

### Sprint 3 — Query API
- Events: `GET /events` (filters: status, paystackEvent, merchantId[super], from, to, page, size),
  `GET /events/{eventId}` (full parsedData, merchant-scoped 404).
- Payments: `GET /payments/{reference}`, `GET /payments` (filters + pagination), merchant-scoped.
- Merchant summary: `GET /merchants/summary` (zeroed if none, not 404),
  `GET /merchants/{merchantId}/summary` (super only; 403 otherwise).
- Dead letters: `GET /dead-letters` (scoped), `POST /dead-letters/{eventId}/retry` (reset, scoped).
- Pagination response shape: `{ content, page, size, totalElements, totalPages, last }`.
- Tests: query service tests for filters, merchant isolation, super-key bypass, pagination, retry reset.
- Gate + commit: `feat: event query API, payment status API, merchant summary, dead letter management`.

### Sprint 4 — Polish, README, deployment
- Code-review pass (dead code, imports, one-line Javadoc on public service methods, no `println`).
- Startup API key seeding when `api_keys` empty: log demo key once at INFO with a clear label.
- `deploy/paytrail.service`, `deploy/nginx.conf`, `deploy/README-deployment.md`.
- `docs/paytrail-api.postman_collection.json` with a pre-built webhook payload + valid test HMAC
  (test secret `test_secret_key_for_demo`, documented in the collection).
- `README.md` (Overview, How it works, Architecture, Tech Stack, Prerequisites, Local Setup,
  Environment Variables, API Reference, Simulating Webhooks, Running Tests, Deployment, License).
- Final `mvn clean test`.
- Gate + commit: `feat: startup seeding, deployment config, Postman collection, README, final polish`.

## 7. Testing Strategy

- Unit: JUnit 5 + Mockito for all service classes.
- Integration: embedded MongoDB.
- Web layer: MockMvc for controllers and the auth filter.
- Redis: mocked `RedisTemplate` beans; behavior asserted via interactions.
- Required coverage: HMAC constant-time path, idempotency no-op on re-send, dead-letter at 3 failures,
  lock prevents concurrent processing, merchant isolation, super-key bypass.

## 8. Error Handling

`GlobalExceptionHandler`:
- `UnauthorizedException` -> 401
- `ResourceNotFoundException` -> 404
- `InvalidSignatureException` -> 400
- `DuplicateEventException` -> 409
- `MethodArgumentNotValidException` -> 400 with field errors
- `Exception` (catch-all) -> 500, full stack trace logged

## 9. Environment Variables

`MONGODB_URI`, `REDIS_URL`, `PAYSTACK_SECRET_KEY`, `SUPER_API_KEY`, `APP_PORT` (8080),
`SCHEDULER_BATCH_SIZE` (50), `SCHEDULER_INTERVAL_MS` (10000). No secrets committed.

## 10. Definition of Done

`mvn clean test` green; constructor injection throughout; no `System.out.println` in `src/main`
(startup seed log excepted); no hardcoded secrets; `ApiResponse<T>` shape on all endpoints;
paginated list shape on all list endpoints; webhook returns 200 in under 200ms with no inline
processing; idempotency, dead-letter, lock, merchant-scoping, and super-key bypass all tested;
Mongo indexes created on startup; README, Postman collection, and deployment files present.
