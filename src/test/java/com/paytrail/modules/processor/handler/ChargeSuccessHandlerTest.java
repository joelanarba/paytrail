package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Test
    void nullParsedDataDoesNotThrow() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        ChargeSuccessHandler handler = new ChargeSuccessHandler(writer);
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m2");
        e.setReference("ref_2");
        e.setParsedData(null);
        assertDoesNotThrow(() -> handler.handle(e));
        verify(writer).upsertPaymentSuccess(eq("ref_2"), eq("m2"), eq(0L), isNull(),
            isNull(), isNull(), isNull(), any());
        verify(writer).recordSuccess(eq("m2"), eq(0L), any());
    }

    @Test
    void missingCustomerYieldsNullEmailAndName() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        ChargeSuccessHandler handler = new ChargeSuccessHandler(writer);
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m3");
        e.setReference("ref_3");
        e.setParsedData(Document.parse("{\"reference\":\"ref_3\",\"amount\":100}"));
        handler.handle(e);
        verify(writer).upsertPaymentSuccess(eq("ref_3"), eq("m3"), eq(100L), isNull(),
            isNull(), isNull(), isNull(), any());
        verify(writer).recordSuccess(eq("m3"), eq(100L), any());
    }

    @Test
    void firstNameOnlyTrimsCorrectly() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        ChargeSuccessHandler handler = new ChargeSuccessHandler(writer);
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m4");
        e.setReference("ref_4");
        e.setParsedData(Document.parse("{\"reference\":\"ref_4\",\"amount\":200,"
            + "\"customer\":{\"first_name\":\"Ada\"}}"));
        handler.handle(e);
        verify(writer).upsertPaymentSuccess(eq("ref_4"), eq("m4"), eq(200L), isNull(),
            isNull(), eq("Ada"), isNull(), any());
        verify(writer).recordSuccess(eq("m4"), eq(200L), any());
    }

    @Test
    void usesPaidAtFromPayloadWhenPresent() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        ChargeSuccessHandler handler = new ChargeSuccessHandler(writer);
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m5");
        e.setReference("ref_5");
        Instant expectedPaidAt = Instant.parse("2026-01-02T03:04:05Z");
        e.setParsedData(Document.parse("{\"reference\":\"ref_5\",\"amount\":300,"
            + "\"paid_at\":\"2026-01-02T03:04:05Z\"}"));
        handler.handle(e);
        verify(writer).upsertPaymentSuccess(eq("ref_5"), eq("m5"), eq(300L), isNull(),
            isNull(), isNull(), isNull(), eq(expectedPaidAt));
        verify(writer).recordSuccess(eq("m5"), eq(300L), eq(expectedPaidAt));
    }
}
