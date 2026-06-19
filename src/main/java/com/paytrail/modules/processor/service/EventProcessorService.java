package com.paytrail.modules.processor.service;

import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.handler.EventHandler;
import com.paytrail.modules.processor.redis.IdempotencyStore;
import com.paytrail.modules.processor.redis.ProcessingLock;
import com.paytrail.repository.WebhookEventRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventProcessorService {
    private static final Logger log = LoggerFactory.getLogger(EventProcessorService.class);
    private static final int MAX_RETRIES = 3;

    private final ProcessingLock lock;
    private final IdempotencyStore idempotency;
    private final WebhookEventRepository repository;
    private final Map<String, EventHandler> handlers = new HashMap<>();

    public EventProcessorService(ProcessingLock lock, IdempotencyStore idempotency,
                                 WebhookEventRepository repository, List<EventHandler> handlerList) {
        this.lock = lock;
        this.idempotency = idempotency;
        this.repository = repository;
        for (EventHandler h : handlerList) {
            handlers.put(h.eventType(), h);
        }
    }

    /** Acquires a Redis lock, checks idempotency, dispatches to the appropriate handler, and marks the event PROCESSED or DEAD_LETTER on failure. */
    public void processEvent(WebhookEvent event) {
        if (!lock.tryAcquire(event.getEventId())) {
            log.info("Lock not acquired for event {}, skipping", event.getEventId());
            return;
        }
        try {
            if (idempotency.isProcessed(event.getReference(), event.getPaystackEvent())) {
                log.info("Event {} already processed (idempotent), marking PROCESSED", event.getEventId());
                markProcessed(event);
                return;
            }

            event.setStatus(EventStatus.PROCESSING);
            event.setUpdatedAt(Instant.now());
            repository.save(event);

            EventHandler handler = handlers.get(event.getPaystackEvent());
            if (handler == null) {
                log.info("No handler for event type {}, marking PROCESSED", event.getPaystackEvent());
            } else {
                handler.handle(event);
            }
            markProcessed(event);
        } catch (Exception e) {
            handleFailure(event, e);
        } finally {
            lock.release(event.getEventId());
        }
    }

    private void markProcessed(WebhookEvent event) {
        event.setStatus(EventStatus.PROCESSED);
        event.setProcessedAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        repository.save(event);
        idempotency.markProcessed(event.getReference(), event.getPaystackEvent());
    }

    private void handleFailure(WebhookEvent event, Exception e) {
        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() >= MAX_RETRIES) {
            event.setStatus(EventStatus.DEAD_LETTER);
            event.setFailureReason(e.getMessage());
            log.error("Event {} moved to DEAD_LETTER after {} retries", event.getEventId(), event.getRetryCount(), e);
        } else {
            event.setStatus(EventStatus.RECEIVED);
            log.error("Event {} failed, retry {} scheduled", event.getEventId(), event.getRetryCount(), e);
        }
        event.setUpdatedAt(Instant.now());
        repository.save(event);
    }
}
