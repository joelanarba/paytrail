package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ChargeSuccessHandlerTest {
    @Test
    void upsertsPaymentAndIncrementsSummary() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        ChargeSuccessHandler handler = new ChargeSuccessHandler(writer);
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m1");
        e.setReference("ref_1");
        e.setParsedData(Document.parse("{\"reference\":\"ref_1\",\"amount\":5000,\"currency\":\"NGN\","
            + "\"customer\":{\"email\":\"a@b.com\",\"first_name\":\"A\",\"last_name\":\"B\"},\"channel\":\"card\"}"));
        handler.handle(e);
        verify(writer).upsertPaymentSuccess(eq("ref_1"), eq("m1"), eq(5000L), eq("NGN"),
            eq("a@b.com"), eq("A B"), eq("card"), any());
        verify(writer).recordSuccess(eq("m1"), eq(5000L), any());
    }
}
