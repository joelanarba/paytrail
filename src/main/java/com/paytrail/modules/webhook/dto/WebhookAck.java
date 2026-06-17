package com.paytrail.modules.webhook.dto;

public class WebhookAck {
    private String eventId;

    public WebhookAck() { }

    public WebhookAck(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() { return eventId; }

    public void setEventId(String eventId) { this.eventId = eventId; }
}
