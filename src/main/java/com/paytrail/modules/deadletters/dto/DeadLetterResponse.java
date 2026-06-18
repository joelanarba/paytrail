package com.paytrail.modules.deadletters.dto;

import java.time.Instant;

public class DeadLetterResponse {
    private String eventId;
    private String paystackEvent;
    private String reference;
    private String merchantId;
    private int retryCount;
    private String failureReason;
    private Instant receivedAt;
    private Instant updatedAt;

    public DeadLetterResponse() { }

    public DeadLetterResponse(String eventId, String paystackEvent, String reference,
                               String merchantId, int retryCount, String failureReason,
                               Instant receivedAt, Instant updatedAt) {
        this.eventId = eventId;
        this.paystackEvent = paystackEvent;
        this.reference = reference;
        this.merchantId = merchantId;
        this.retryCount = retryCount;
        this.failureReason = failureReason;
        this.receivedAt = receivedAt;
        this.updatedAt = updatedAt;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getPaystackEvent() { return paystackEvent; }
    public void setPaystackEvent(String paystackEvent) { this.paystackEvent = paystackEvent; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
