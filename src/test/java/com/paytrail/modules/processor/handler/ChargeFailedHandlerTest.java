package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ChargeFailedHandlerTest {
    @Test
    void marksFailedAndIncrementsFailure() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m1"); e.setReference("ref_1");
        e.setParsedData(Document.parse("{\"reference\":\"ref_1\",\"gateway_response\":\"declined\"}"));
        new ChargeFailedHandler(writer).handle(e);
        verify(writer).upsertPaymentFailed("ref_1", "m1", "declined");
        verify(writer).recordFailure(eq("m1"), any());
    }
}
