package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;

public interface EventHandler {
    String eventType();
    void handle(WebhookEvent event);
}
