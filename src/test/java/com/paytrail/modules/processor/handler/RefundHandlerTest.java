package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class RefundHandlerTest {
    @Test
    void marksRefundedAndRecordsRefund() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m1"); e.setReference("ref_1");
        e.setParsedData(Document.parse("{\"reference\":\"ref_1\",\"amount\":5000}"));
        new RefundHandler(writer).handle(e);
        verify(writer).upsertPaymentRefunded("ref_1");
        verify(writer).recordRefund("m1", 5000L);
    }
}
