package com.paytrail.modules.processor.redis;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyStore {
    private static final Duration TTL = Duration.ofDays(7);
    private final StringRedisTemplate redis;

    public IdempotencyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String reference, String event) {
        return "idempotency:" + reference + ":" + event;
    }

    /** Returns true if the given reference/event combination has already been marked PROCESSED in Redis. */
    public boolean isProcessed(String reference, String event) {
        return "PROCESSED".equals(redis.opsForValue().get(key(reference, event)));
    }

    /** Stores a PROCESSED marker in Redis with a 7-day TTL for the given reference/event pair. */
    public void markProcessed(String reference, String event) {
        redis.opsForValue().set(key(reference, event), "PROCESSED", TTL);
    }
}
