package com.paytrail.modules.processor.service;

import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.handler.EventHandler;
import com.paytrail.modules.processor.redis.IdempotencyStore;
import com.paytrail.modules.processor.redis.ProcessingLock;
import com.paytrail.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventProcessorServiceTest {
    private ProcessingLock lock;
    private IdempotencyStore idempotency;
    private WebhookEventRepository repo;
    private EventHandler chargeSuccess;
    private EventProcessorService service;

    @BeforeEach
    void setup() {
        lock = mock(ProcessingLock.class);
        idempotency = mock(IdempotencyStore.class);
        repo = mock(WebhookEventRepository.class);
        chargeSuccess = mock(EventHandler.class);
        when(chargeSuccess.eventType()).thenReturn("charge.success");
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new EventProcessorService(lock, idempotency, repo, List.of(chargeSuccess));
    }

    private WebhookEvent event() {
        WebhookEvent e = new WebhookEvent();
        e.setEventId("e1"); e.setReference("ref1"); e.setPaystackEvent("charge.success");
        e.setStatus(EventStatus.RECEIVED);
        return e;
    }

    @Test
    void happyPathProcessesAndMarksProcessed() {
        when(lock.tryAcquire("e1")).thenReturn(true);
        when(idempotency.isProcessed("ref1", "charge.success")).thenReturn(false);
        WebhookEvent e = event();
        service.processEvent(e);
        verify(chargeSuccess).handle(e);
        verify(idempotency).markProcessed("ref1", "charge.success");
        verify(lock).release("e1");
        assertEquals(EventStatus.PROCESSED, e.getStatus());
        assertNotNull(e.getProcessedAt());
    }

    @Test
    void skipsWhenLockNotAcquired() {
        when(lock.tryAcquire("e1")).thenReturn(false);
        WebhookEvent e = event();
        service.processEvent(e);
        verify(chargeSuccess, never()).handle(any());
        verify(lock, never()).release(anyString());
    }

    @Test
    void idempotentNoOpMarksProcessedWithoutHandler() {
        when(lock.tryAcquire("e1")).thenReturn(true);
        when(idempotency.isProcessed("ref1", "charge.success")).thenReturn(true);
        WebhookEvent e = event();
        service.processEvent(e);
        verify(chargeSuccess, never()).handle(any());
        assertEquals(EventStatus.PROCESSED, e.getStatus());
        verify(lock).release("e1");
    }

    @Test
    void failureIncrementsRetryAndResetsToReceived() {
        when(lock.tryAcquire("e1")).thenReturn(true);
        when(idempotency.isProcessed(anyString(), anyString())).thenReturn(false);
        doThrow(new RuntimeException("boom")).when(chargeSuccess).handle(any());
        WebhookEvent e = event();
        service.processEvent(e);
        assertEquals(1, e.getRetryCount());
        assertEquals(EventStatus.RECEIVED, e.getStatus());
        verify(lock).release("e1");
    }

    @Test
    void thirdFailureMovesToDeadLetter() {
        when(lock.tryAcquire("e1")).thenReturn(true);
        when(idempotency.isProcessed(anyString(), anyString())).thenReturn(false);
        doThrow(new RuntimeException("boom")).when(chargeSuccess).handle(any());
        WebhookEvent e = event();
        e.setRetryCount(2);
        service.processEvent(e);
        assertEquals(3, e.getRetryCount());
        assertEquals(EventStatus.DEAD_LETTER, e.getStatus());
        assertEquals("boom", e.getFailureReason());
    }

    @Test
    void unknownEventTypeMarkedProcessed() {
        when(lock.tryAcquire("e1")).thenReturn(true);
        when(idempotency.isProcessed(anyString(), anyString())).thenReturn(false);
        WebhookEvent e = event();
        e.setPaystackEvent("charge.unknown");
        service.processEvent(e);
        assertEquals(EventStatus.PROCESSED, e.getStatus());
        verify(idempotency).markProcessed("ref1", "charge.unknown");
    }
}
