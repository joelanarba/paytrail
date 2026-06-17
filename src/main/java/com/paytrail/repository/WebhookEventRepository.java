package com.paytrail.repository;

import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WebhookEventRepository extends MongoRepository<WebhookEvent, String> {
    List<WebhookEvent> findByStatus(EventStatus status);

    Optional<WebhookEvent> findByReferenceAndPaystackEvent(String reference, String paystackEvent);

    List<WebhookEvent> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetries, Pageable pageable);

    Optional<WebhookEvent> findByEventId(String eventId);

    Page<WebhookEvent> findByMerchantId(String merchantId, Pageable pageable);
}
