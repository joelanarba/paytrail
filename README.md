# PayTrail

PayTrail is a standalone payment webhook event processor built for Paystack-integrated platforms. It receives webhook events from Paystack, stores them immediately as immutable documents in MongoDB, then processes them asynchronously in the background with idempotency guarantees and automatic retries.

## Overview

Most webhook integrations process events inline: when Paystack delivers a webhook, the application verifies it, runs business logic, writes to the database, and only then returns a response. If any of those steps are slow or fail, Paystack sees a timeout or error and retries — leading to duplicate processing, inconsistent state, and harder-to-debug failures.

PayTrail separates ingestion from processing. The webhook endpoint verifies the signature and persists the raw event to MongoDB in a single fast write, then returns `200 OK` immediately. A background scheduler handles all business logic asynchronously, with Redis-backed idempotency keys and per-event processing locks to ensure each event is processed exactly once regardless of retries or concurrent scheduler runs.

## How It Works

**Ingestion** — `POST /api/v1/webhooks/paystack` accepts the raw Paystack payload, verifies the HMAC-SHA512 signature, generates an internal `eventId` (UUID), and writes the event to MongoDB with status `RECEIVED`. The response is returned within milliseconds.

**Background processing** — A Spring scheduler runs every 10 seconds (configurable). It fetches a batch of up to 50 `RECEIVED` events and dispatches each to a thread pool for processing.

**Idempotency** — Before processing each event, the processor checks a Redis key `idempotency:{reference}:{paystackEvent}`. If the key is set to `PROCESSED`, the event is skipped and marked processed without running the handler again. After successful processing, the key is set with a 7-day TTL.

**Processing lock** — Before handling an event, the processor sets a Redis key `lock:event:{eventId}` with a 30-second TTL using a SET NX operation. If the lock cannot be acquired (another instance already holds it), the event is skipped until the next scheduler cycle.

**Handlers** — Each event type routes to a dedicated handler:
- `charge.success` — creates or updates the payment projection, increments merchant revenue counters
- `charge.failed` — records the failure reason on the payment projection
- `refund.processed` — marks the payment as refunded, updates merchant refund totals
- `transfer.success` / `transfer.failed` — records transfer outcomes

**Retry and dead letters** — On handler failure, `retryCount` is incremented and the event is set back to `RECEIVED` so it is picked up on the next scheduler run. After 3 consecutive failures, the event is moved to `DEAD_LETTER` status with the failure reason recorded. Dead-letter events are queryable and can be manually re-queued via the API.

**Query API** — All query endpoints are protected by an `X-Api-Key` header. Each API key is scoped to a `merchantId`; queries return only that merchant's data. A super key (from the `SUPER_API_KEY` environment variable) bypasses merchant scoping for admin access.

## Architecture

```
Paystack
  |
  POST /api/v1/webhooks/paystack
  |
  +-- Verify HMAC-SHA512 signature (x-paystack-signature header)
  +-- Store raw event to MongoDB  (status: RECEIVED)
  +-- Return 200 OK immediately
  |
  [EventProcessorScheduler -- runs every 10 seconds]
  |
  +-- Fetch batch of RECEIVED events (up to 50)
  |
  +-- Per event (parallel, thread pool):
        Acquire Redis lock  (lock:event:{eventId}, 30s TTL)
        Check idempotency   (idempotency:{reference}:{event})
        Route to handler:
          charge.success     -> PaymentProjection (SUCCESS) + MerchantSummary
          charge.failed      -> PaymentProjection (FAILED)
          refund.processed   -> PaymentProjection (REFUNDED) + MerchantSummary
          transfer.success / transfer.failed -> recorded on event
        Mark PROCESSED, set idempotency key (7-day TTL)
        Release lock
        On failure: retryCount++, status -> RECEIVED
        After 3 failures: status -> DEAD_LETTER
  |
Query API (X-Api-Key, merchant-scoped)
  |
  +-- GET  /api/v1/events
  +-- GET  /api/v1/events/{eventId}
  +-- GET  /api/v1/payments
  +-- GET  /api/v1/payments/{reference}
  +-- GET  /api/v1/merchants/summary
  +-- GET  /api/v1/merchants/{merchantId}/summary  (super key only)
  +-- GET  /api/v1/dead-letters
  +-- POST /api/v1/dead-letters/{eventId}/retry
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.2 |
| Database | MongoDB (Spring Data MongoDB) |
| Cache / locks | Redis (Spring Data Redis, Lettuce client) |
| Build | Maven 3 |
| Testing | JUnit 5, Mockito, embedded MongoDB (de.flapdoodle 4.16.1) |

## Prerequisites

- Java 21 (JDK)
- Maven 3.8+
- A MongoDB instance (MongoDB Atlas free tier works)
- A Redis instance (Upstash free tier works, or a local Redis server)
- A Paystack account (for the secret key used to verify webhook signatures)

## Local Setup

1. Clone the repository:

```bash
git clone https://github.com/your-username/paytrail.git
cd paytrail
```

2. Create a local environment file or export the variables listed in the Environment Variables section. At minimum you need `MONGODB_URI`, `REDIS_URL`, `PAYSTACK_SECRET_KEY`, and `SUPER_API_KEY`.

3. Start the application:

```bash
mvn spring-boot:run
```

4. On first startup, if the `api_keys` collection in MongoDB is empty, the application seeds a demo API key for merchant `demo-merchant` and prints it once to the INFO log:

```
[PAYTRAIL STARTUP] Demo API key for merchant 'demo-merchant': pt_live_xxxxxxxxxxxxxxxxxxxx
```

Copy that key and use it as the `X-Api-Key` header for all query API requests. The key is stored hashed in MongoDB; if you lose it, delete the document from `api_keys` and restart the application to generate a new one.

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `MONGODB_URI` | MongoDB connection string (e.g. `mongodb+srv://user:pass@cluster.mongodb.net/paytrail`) | required |
| `REDIS_URL` | Redis connection URL (e.g. `redis://localhost:6379` or `rediss://...` for TLS) | required |
| `PAYSTACK_SECRET_KEY` | Paystack secret key used to verify incoming webhook signatures | required |
| `SUPER_API_KEY` | Super key that grants admin access to the query API, bypassing merchant scoping | required |
| `APP_PORT` | Port the HTTP server listens on | `8080` |
| `SCHEDULER_BATCH_SIZE` | Maximum number of events the scheduler processes per cycle | `50` |
| `SCHEDULER_INTERVAL_MS` | Milliseconds between scheduler runs | `10000` |

Never commit real values for these variables. Do not add a `.env` file to version control.

## API Reference

All responses follow this shape:

```json
{
  "success": true,
  "message": "Human-readable message",
  "data": { }
}
```

Error responses:

```json
{
  "success": false,
  "message": "What went wrong",
  "errors": [ ]
}
```

### Webhooks

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/webhooks/paystack` | None (HMAC-SHA512 signature on `x-paystack-signature` header) | Receive a Paystack webhook event |

### Events

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/events` | `X-Api-Key` (merchant-scoped) | List webhook events. Query params: `status`, `paystackEvent`, `merchantId`, `from`, `to`, `page`, `size` |
| `GET` | `/api/v1/events/{eventId}` | `X-Api-Key` (merchant-scoped) | Get a single webhook event by internal event ID |

### Payments

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/payments/{reference}` | `X-Api-Key` (merchant-scoped) | Get payment status by Paystack transaction reference |
| `GET` | `/api/v1/payments` | `X-Api-Key` (merchant-scoped) | List payments. Query params: `status`, `channel`, `merchantId`, `from`, `to`, `page`, `size` |

### Merchants

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/merchants/summary` | `X-Api-Key` (merchant-scoped) | Get the summary for the merchant associated with the calling API key |
| `GET` | `/api/v1/merchants/{merchantId}/summary` | `X-Api-Key` (super key only) | Get the summary for any merchant by ID |

### Dead Letters

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/dead-letters` | `X-Api-Key` (merchant-scoped) | List dead-letter events. Query params: `merchantId`, `page`, `size` |
| `POST` | `/api/v1/dead-letters/{eventId}/retry` | `X-Api-Key` (merchant-scoped) | Re-queue a dead-letter event for processing (sets status back to RECEIVED) |

### Dev / Admin

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/dev/api-keys` | `X-Super-Key` header | Create a new API key for a merchant. Body: `{ "merchantId": "...", "description": "..." }` |

## Simulating Webhooks

I have included a Postman collection at `docs/paytrail-api.postman_collection.json`. Import it into Postman to get pre-built requests for all endpoints.

The Webhooks folder contains a `charge.success` request with a pre-signed payload. The payload is signed using the test secret `test_secret_key_for_demo`. To accept it locally, start the application with that value as your Paystack secret:

```bash
PAYSTACK_SECRET_KEY=test_secret_key_for_demo mvn spring-boot:run
```

Then send the request from Postman. The event will be ingested and processed by the background scheduler within 10 seconds.

## Running Tests

```bash
mvn test
```

The test suite uses:
- **Embedded MongoDB** (de.flapdoodle) — no external MongoDB instance required
- **Mocked Redis** — `StringRedisTemplate` is mocked with Mockito; no external Redis instance required

What is covered:
- HMAC-SHA512 signature verification (valid, tampered, and missing signatures)
- API key auth filter routing (missing key returns 401, invalid key returns 401, valid key passes, webhook and dev paths are excluded)
- Idempotency: sending the same event twice results in it being processed only once
- Retry logic: a handler that fails three times moves the event to `DEAD_LETTER`
- Dead letter re-queue: re-queued event status is reset to `RECEIVED`
- Merchant scoping: a merchant API key cannot access another merchant's data
- Per-handler behaviour: `charge.success`, `charge.failed`, `refund.processed`, `transfer.success`, `transfer.failed`
- Projection writes: payment projections and merchant summaries are updated correctly

## Deployment

See [deploy/README-deployment.md](deploy/README-deployment.md) for step-by-step instructions covering EC2 setup, Java 21 installation, systemd service configuration, Nginx reverse proxy, and JAR deployment.

## License

MIT
