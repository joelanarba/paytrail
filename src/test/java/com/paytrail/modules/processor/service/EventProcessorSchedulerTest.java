package com.paytrail.modules.processor.service;

import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventProcessorSchedulerTest {
    @Test
    void submitsEachReceivedEvent() {
        WebhookEventRepository repo = mock(WebhookEventRepository.class);
        EventProcessorService processor = mock(EventProcessorService.class);
        WebhookEvent e1 = new WebhookEvent(); e1.setEventId("e1");
        WebhookEvent e2 = new WebhookEvent(); e2.setEventId("e2");
        when(repo.findByStatusAndRetryCountLessThan(eq(EventStatus.RECEIVED), eq(3), any(Pageable.class)))
            .thenReturn(List.of(e1, e2));
        EventProcessorScheduler scheduler = new EventProcessorScheduler(repo, processor, Runnable::run, 50);
        scheduler.runBatch();
        verify(processor).processEvent(e1);
        verify(processor).processEvent(e2);
    }
}
