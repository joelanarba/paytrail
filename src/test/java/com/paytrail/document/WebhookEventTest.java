package com.paytrail.document;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebhookEventTest {

    @Test
    void storesFields() {
        WebhookEvent e = new WebhookEvent();
        e.setStatus(EventStatus.RECEIVED);
        e.setReference("ref_1");
        assertEquals(EventStatus.RECEIVED, e.getStatus());
        assertEquals("ref_1", e.getReference());
    }
}
