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

    public boolean isProcessed(String reference, String event) {
        return "PROCESSED".equals(redis.opsForValue().get(key(reference, event)));
    }

    public void markProcessed(String reference, String event) {
        redis.opsForValue().set(key(reference, event), "PROCESSED", TTL);
    }
}
