package com.paytrail.modules.webhook.service;

import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.exception.InvalidSignatureException;
import com.paytrail.repository.WebhookEventRepository;
import com.paytrail.util.EventParser;
import com.paytrail.util.HmacUtil;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);

    private final WebhookEventRepository repository;
    private final EventParser eventParser;
    private final String paystackSecret;

    public WebhookIngestionService(WebhookEventRepository repository,
                                   EventParser eventParser,
                                   @Value("${paytrail.paystack.secret-key}") String paystackSecret) {
        this.repository = repository;
        this.eventParser = eventParser;
        this.paystackSecret = paystackSecret;
    }

    /** Verifies the Paystack HMAC signature, persists the raw webhook event, and returns the internal event ID. */
    public String ingest(String rawBody, String signature) {
        if (!HmacUtil.verifySignature(rawBody, paystackSecret, signature)) {
            throw new InvalidSignatureException("Invalid webhook signature");
        }
        EventParser.ParsedEvent parsed = eventParser.parse(rawBody);
        Instant now = Instant.now();
        WebhookEvent event = new WebhookEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setPaystackEvent(parsed.event());
        event.setReference(parsed.reference());
        event.setMerchantId(parsed.merchantId());
        event.setRawPayload(rawBody);
        event.setParsedData(parsed.data());
        event.setStatus(EventStatus.RECEIVED);
        event.setRetryCount(0);
        event.setReceivedAt(now);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        repository.save(event);
        log.info("Ingested webhook event {} type={} ref={}", event.getEventId(), event.getPaystackEvent(), event.getReference());
        return event.getEventId();
    }
}
