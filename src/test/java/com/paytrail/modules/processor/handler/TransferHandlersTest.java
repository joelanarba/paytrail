package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferHandlersTest {
    @Test
    void transferHandlersExposeTypesAndRunWithoutError() {
        WebhookEvent e = new WebhookEvent();
        e.setReference("trf_1");
        e.setParsedData(Document.parse("{\"reference\":\"trf_1\",\"amount\":1000}"));
        TransferSuccessHandler s = new TransferSuccessHandler();
        TransferFailedHandler f = new TransferFailedHandler();
        assertEquals("transfer.success", s.eventType());
        assertEquals("transfer.failed", f.eventType());
        s.handle(e);
        f.handle(e);
    }
}
