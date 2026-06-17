package com.paytrail.modules.processor.redis;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProcessingLock {
    private static final Duration TTL = Duration.ofSeconds(30);
    private final StringRedisTemplate redis;

    public ProcessingLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean tryAcquire(String eventId) {
        Boolean ok = redis.opsForValue().setIfAbsent("lock:event:" + eventId, "1", TTL);
        return Boolean.TRUE.equals(ok);
    }

    public void release(String eventId) {
        redis.delete("lock:event:" + eventId);
    }
}
