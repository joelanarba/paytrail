package com.paytrail.modules.processor.redis;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessingLockTest {
    @SuppressWarnings("unchecked")
    @Test
    void acquiresWhenKeyAbsent() {
        StringRedisTemplate t = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(t.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(eq("lock:event:e1"), eq("1"), any(Duration.class))).thenReturn(true);
        ProcessingLock lock = new ProcessingLock(t);
        assertTrue(lock.tryAcquire("e1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void failsWhenKeyPresent() {
        StringRedisTemplate t = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(t.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertFalse(new ProcessingLock(t).tryAcquire("e1"));
    }
}
