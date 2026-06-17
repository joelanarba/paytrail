package com.paytrail.modules.processor.redis;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdempotencyStoreTest {
    @SuppressWarnings("unchecked")
    @Test
    void reportsProcessedWhenValueMatches() {
        StringRedisTemplate t = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(t.opsForValue()).thenReturn(ops);
        when(ops.get("idempotency:ref1:charge.success")).thenReturn("PROCESSED");
        assertTrue(new IdempotencyStore(t).isProcessed("ref1", "charge.success"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void marksProcessedWithTtl() {
        StringRedisTemplate t = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(t.opsForValue()).thenReturn(ops);
        new IdempotencyStore(t).markProcessed("ref1", "charge.success");
        verify(ops).set(eq("idempotency:ref1:charge.success"), eq("PROCESSED"), eq(Duration.ofDays(7)));
    }
}
