# PayTrail — Build Design

Date: 2026-06-17
Status: Approved
Source of truth: this document + `CLAUDE.md` + the build prompt it captures.

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

## 3. Resolved Technical Decisions

These resolve open questions and internal contradictions found in the build prompt.

| Area | Decision | Rationale |
|---|---|---|
| Auth implementation | Plain `OncePerRequestFilter` (`ApiKeyAuthFilter`) registered via `FilterRegistrationBean`. **No** `spring-boot-starter-security`. | The build prompt's dependency list omits Spring Security, but Task 1.4 referenced CSRF/form-login (Spring Security concepts). A plain filter is simpler, fewer deps, and fits a service-to-service API-key model. `SecurityConfig` only registers the filter; there is no CSRF/form-login to disable. |
| Super key | Recognized inside the same `ApiKeyAuthFilter`: if `X-Api-Key` equals `SUPER_API_KEY`, set super context (`isSuperKey = true`) which bypasses merchant scoping. | One auth path, no separate mechanism. |
| Auth scope | Filter applies to all routes **except** `/api/v1/webhooks/**` and `/api/v1/dev/**`. The dev seed endpoint validates `X-Super-Key` against `SUPER_API_KEY` itself. | Webhook is HMAC-secured; dev endpoint is super-key-gated by its own check. |
| Auth failure response | Filter writes a `401` `ApiResponse` error JSON directly to the response. | Consistent error shape without Spring Security entry points. |
| Redis in tests | Mocked `RedisTemplate` beans via a test `@TestConfiguration`; lock/idempotency asserted via Mockito interactions. Real Lettuce client at runtime. | No maintained embedded Redis for Java 21; deployment is Docker-free so Testcontainers is unreliable in CI/EC2. Hermetic and fast. |
| HMAC raw body | Controller takes `@RequestBody String rawBody`, verifies HMAC on the exact string, then parses with `ObjectMapper`. | Simpler and more robust than registering a custom `HttpMessageConverter` / content-caching wrapper. |
| Redis lock primitive | `redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(30))` returning `Boolean`. | Native `SET NX EX` semantics. |
| Document IDs | `@Id` with `String` type. | Rule 11 overrides CLAUDE.md's `ObjectId` schema notation. |
| Mongo indexes | Created on startup in `MongoConfig` via `IndexOperations`, with an INFO log confirming each. | Verifiable on boot; no reliance on annotation auto-index. |
| Timestamps | `java.time.Instant`, UTC. | Rule 10. |
| Git | `git init` first; `.gitignore` covers `CLAUDE.md`, `.env`, `target/`, IDE files. Conventional commits, no `Co-Authored-By`, no AI references. | No repo exists yet; final checklist requires `CLAUDE.md` gitignored. |

## 4. Non-Negotiable Rules (apply to every sprint)

1. No Lombok — explicit constructors, getters, setters.
2. No MapStruct — manual DTO mapping in the service layer.
3. Constructor injection only — never `@Autowired` on a field.
4. No `System.out.println` — SLF4J only (the Sprint 4 startup key seed log is the single labelled exception).
5. No hardcoded secrets — all sensitive values from environment variables.
6. No `Co-Authored-By` trailers in commits.
7. No mention of AI tools / code generators in commits, comments, or files.
8. No emojis anywhere.
9. All endpoints wrapped in `ResponseEntity<ApiResponse<T>>`.
10. All timestamps stored as UTC `java.time.Instant`.
11. MongoDB document IDs use `@Id` with `String` type.
12. Every sprint ends with `mvn clean test` green and no compiler warnings before proceeding.

`ApiResponse<T>` success shape: `{ success: true, message: string, data: T }`.
Error shape: `{ success: false, message: string, errors: [...] }`.

## 5. Data Model (MongoDB)

- **`webhook_events`** — immutable raw events. Fields per CLAUDE.md. Status enum:
  `RECEIVED | PROCESSING | PROCESSED | FAILED | DEAD_LETTER`. Indexes: compound
  `(reference, paystackEvent)`, `status`, `merchantId`.
- **`payment_projections`** — current payment state. Status: `PENDING | SUCCESS | FAILED | REFUNDED`.
  Indexes: unique `reference`, `merchantId`. Amounts in kobo (`Long`).
- **`merchant_summaries`** — per-merchant aggregates. Index: unique `merchantId`.
- **`api_keys`** — `keyHash` (SHA-256, unique index), `merchantId`, `isActive`, `lastUsedAt`.

Upserts in handlers use `MongoTemplate.upsert(query, update, Class)` (not repository `save`),
always setting `updatedAt`.

## 6. Sprint Breakdown (single plan, executed in order)

### Sprint 1 — Scaffold, MongoDB, API key auth, webhook ingestion
- Maven project: Spring Boot 3.x, Java 21. Deps: web, data-mongodb, data-redis, validation,
  flapdoodle embed mongo (test). `application.properties` env-driven;
  `application-test.properties` uses embedded Mongo + mocked Redis.
- Document classes: `WebhookEvent`, `PaymentProjection`, `MerchantSummary`, `ApiKey`.
- `MongoConfig` creates indexes on startup with log confirmation.
- Repositories with the finder methods listed in the build prompt.
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
- `README.md` (12 sections, first person, no emojis, no AI references).
- Final `mvn clean test`.
- Gate + commit: `feat: startup seeding, deployment config, Postman collection, README, final polish`.

## 7. Testing Strategy

- Unit: JUnit 5 + Mockito for all service classes.
- Integration: flapdoodle embedded MongoDB.
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

The build prompt's final verification checklist applies in full: `mvn clean test` green;
no `println`/Lombok/field-`@Autowired`/hardcoded secrets/AI references/emojis; `ApiResponse`
shape on all endpoints; paginated list shape; webhook <200ms; idempotency, dead-letter, lock,
merchant-scoping, and super-key bypass all tested; Mongo indexes created on startup;
`CLAUDE.md` gitignored; README + Postman + deploy files present.
