package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;

public interface EventHandler {
    /** Returns the Paystack event type string this handler is responsible for (e.g. "charge.success"). */
    String eventType();
    /** Processes the given webhook event and updates projections as needed. */
    void handle(WebhookEvent event);
}
