package com.paytrail.document;

import java.time.Instant;
import org.bson.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

@org.springframework.data.mongodb.core.mapping.Document(collection = "webhook_events")
@CompoundIndex(name = "reference_event", def = "{'reference': 1, 'paystackEvent': 1}")
public class WebhookEvent {

    @Id private String id;
    @Indexed private String eventId;
    private String paystackEvent;
    private String reference;
    @Indexed private String merchantId;
    private String rawPayload;
    private Document parsedData;
    @Indexed private EventStatus status;
    private int retryCount;
    private String failureReason;
    private Instant receivedAt;
    private Instant processedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public WebhookEvent() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getPaystackEvent() { return paystackEvent; }
    public void setPaystackEvent(String paystackEvent) { this.paystackEvent = paystackEvent; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public Document getParsedData() { return parsedData; }
    public void setParsedData(Document parsedData) { this.parsedData = parsedData; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
