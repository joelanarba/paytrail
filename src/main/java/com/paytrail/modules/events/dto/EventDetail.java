package com.paytrail.modules.events.dto;

import com.paytrail.document.EventStatus;
import java.time.Instant;
import org.bson.Document;

public class EventDetail {
    private final String eventId;
    private final String paystackEvent;
    private final String reference;
    private final EventStatus status;
    private final int retryCount;
    private final Instant receivedAt;
    private final Instant processedAt;
    private final String merchantId;
    private final Document parsedData;
    private final String failureReason;

    public EventDetail(String eventId, String paystackEvent, String reference, EventStatus status,
                       int retryCount, Instant receivedAt, Instant processedAt,
                       String merchantId, Document parsedData, String failureReason) {
        this.eventId = eventId;
        this.paystackEvent = paystackEvent;
        this.reference = reference;
        this.status = status;
        this.retryCount = retryCount;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
        this.merchantId = merchantId;
        this.parsedData = parsedData;
        this.failureReason = failureReason;
    }

    public String getEventId() { return eventId; }
    public String getPaystackEvent() { return paystackEvent; }
    public String getReference() { return reference; }
    public EventStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public String getMerchantId() { return merchantId; }
    public Document getParsedData() { return parsedData; }
    public String getFailureReason() { return failureReason; }
}
