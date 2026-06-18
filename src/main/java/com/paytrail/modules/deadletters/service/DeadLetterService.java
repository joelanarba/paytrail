package com.paytrail.modules.deadletters.service;

import com.paytrail.common.PageResponse;
import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.exception.ResourceNotFoundException;
import com.paytrail.filter.MerchantContext;
import com.paytrail.modules.deadletters.dto.DeadLetterResponse;
import com.paytrail.repository.WebhookEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class DeadLetterService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterService.class);

    private final MongoTemplate mongo;
    private final WebhookEventRepository repository;

    public DeadLetterService(MongoTemplate mongo, WebhookEventRepository repository) {
        this.mongo = mongo;
        this.repository = repository;
    }

    public PageResponse<DeadLetterResponse> list(String merchantIdFilter, int page, int size) {
        List<Criteria> parts = new ArrayList<>();
        parts.add(Criteria.where("status").is(EventStatus.DEAD_LETTER));
        if (!MerchantContext.isSuperKey()) {
            parts.add(Criteria.where("merchantId").is(MerchantContext.getMerchantId()));
        } else if (merchantIdFilter != null) {
            parts.add(Criteria.where("merchantId").is(merchantIdFilter));
        }
        Criteria c = new Criteria().andOperator(parts.toArray(new Criteria[0]));
        Query query = new Query(c);
        long total = mongo.count(query, WebhookEvent.class);
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        List<DeadLetterResponse> content = mongo.find(query, WebhookEvent.class)
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(content, page, size, total);
    }

    public void retry(String eventId) {
        WebhookEvent e = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Dead letter not found"));
        if (!MerchantContext.isSuperKey() && !e.getMerchantId().equals(MerchantContext.getMerchantId())) {
            throw new ResourceNotFoundException("Dead letter not found");
        }
        e.setStatus(EventStatus.RECEIVED);
        e.setRetryCount(0);
        e.setFailureReason(null);
        e.setUpdatedAt(Instant.now());
        repository.save(e);
        log.info("Dead letter event {} re-queued for processing", eventId);
    }

    private DeadLetterResponse toResponse(WebhookEvent e) {
        return new DeadLetterResponse(
                e.getEventId(),
                e.getPaystackEvent(),
                e.getReference(),
                e.getMerchantId(),
                e.getRetryCount(),
                e.getFailureReason(),
                e.getReceivedAt(),
                e.getUpdatedAt());
    }
}
