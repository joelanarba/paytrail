package com.paytrail.modules.events.service;

import com.paytrail.common.PageResponse;
import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.exception.ResourceNotFoundException;
import com.paytrail.filter.MerchantContext;
import com.paytrail.modules.events.dto.EventDetail;
import com.paytrail.modules.events.dto.EventSummary;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class EventQueryService {

    public static class EventQueryFilters {
        public EventStatus status;
        public String paystackEvent;
        public String merchantId;
        public Instant from;
        public Instant to;
    }

    private final MongoTemplate mongo;

    public EventQueryService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Returns a paginated list of webhook events, filtered by the supplied criteria and scoped to the authenticated merchant. */
    public PageResponse<EventSummary> list(EventQueryFilters f, int page, int size) {
        List<Criteria> parts = new java.util.ArrayList<>();
        if (!MerchantContext.isSuperKey()) {
            parts.add(Criteria.where("merchantId").is(MerchantContext.getMerchantId()));
        } else if (f.merchantId != null) {
            parts.add(Criteria.where("merchantId").is(f.merchantId));
        }
        if (f.status != null) {
            parts.add(Criteria.where("status").is(f.status));
        }
        if (f.paystackEvent != null) {
            parts.add(Criteria.where("paystackEvent").is(f.paystackEvent));
        }
        if (f.from != null || f.to != null) {
            Criteria t = Criteria.where("receivedAt");
            if (f.from != null) t.gte(f.from);
            if (f.to != null) t.lte(f.to);
            parts.add(t);
        }
        Criteria c = parts.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(parts.toArray(new Criteria[0]));
        Query query = new Query(c);
        long total = mongo.count(query, WebhookEvent.class);
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt")));
        List<EventSummary> content = mongo.find(query, WebhookEvent.class)
                .stream()
                .map(this::toSummary)
                .toList();
        return new PageResponse<>(content, page, size, total);
    }

    /** Retrieves full details for a single webhook event by its internal event ID, enforcing merchant scoping. */
    public EventDetail getByEventId(String eventId) {
        Query q = new Query(Criteria.where("eventId").is(eventId));
        WebhookEvent e = mongo.findOne(q, WebhookEvent.class);
        if (e == null) {
            throw new ResourceNotFoundException("Event not found");
        }
        if (!MerchantContext.isSuperKey() && !e.getMerchantId().equals(MerchantContext.getMerchantId())) {
            throw new ResourceNotFoundException("Event not found");
        }
        return new EventDetail(
                e.getEventId(), e.getPaystackEvent(), e.getReference(), e.getStatus(),
                e.getRetryCount(), e.getReceivedAt(), e.getProcessedAt(),
                e.getMerchantId(), e.getParsedData(), e.getFailureReason());
    }

    private EventSummary toSummary(WebhookEvent e) {
        return new EventSummary(
                e.getEventId(), e.getPaystackEvent(), e.getReference(), e.getStatus(),
                e.getRetryCount(), e.getReceivedAt(), e.getProcessedAt());
    }
}
