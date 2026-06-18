package com.paytrail.modules.processor.service;

import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.repository.WebhookEventRepository;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class EventProcessorScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventProcessorScheduler.class);

    private final WebhookEventRepository repository;
    private final EventProcessorService processor;
    private final Executor executor;
    private final int batchSize;

    public EventProcessorScheduler(WebhookEventRepository repository,
                                   EventProcessorService processor,
                                   @Qualifier("eventExecutor") Executor executor,
                                   @Value("${paytrail.scheduler.batch-size}") int batchSize) {
        this.repository = repository;
        this.processor = processor;
        this.executor = executor;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${paytrail.scheduler.interval-ms}")
    public void runBatch() {
        List<WebhookEvent> batch = repository.findByStatusAndRetryCountLessThan(
                EventStatus.RECEIVED, 3, PageRequest.of(0, batchSize));
        log.debug("Scheduler picking up {} events for processing", batch.size());
        for (WebhookEvent event : batch) {
            executor.execute(() -> processor.processEvent(event));
        }
    }
}
