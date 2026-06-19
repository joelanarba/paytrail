package com.paytrail.modules.processor.handler;

import com.paytrail.document.PaymentProjection;
import com.paytrail.document.PaymentStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import com.paytrail.repository.PaymentProjectionRepository;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChargeFailedHandlerTest {

    @Test
    void marksFailedAndIncrementsFailure() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        PaymentProjectionRepository paymentRepository = mock(PaymentProjectionRepository.class);
        when(paymentRepository.findByReference(anyString())).thenReturn(Optional.empty());
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m1"); e.setReference("ref_1");
        e.setParsedData(Document.parse("{\"reference\":\"ref_1\",\"gateway_response\":\"declined\"}"));
        new ChargeFailedHandler(writer, paymentRepository).handle(e);
        verify(writer).upsertPaymentFailed("ref_1", "m1", "declined");
        verify(writer).recordFailure(eq("m1"), any());
    }

    @Test
    void doesNotIncrementCounterWhenAlreadyFailed() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        PaymentProjectionRepository paymentRepository = mock(PaymentProjectionRepository.class);
        PaymentProjection existing = new PaymentProjection();
        existing.setReference("ref_dup");
        existing.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findByReference("ref_dup")).thenReturn(Optional.of(existing));
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m2"); e.setReference("ref_dup");
        e.setParsedData(Document.parse("{\"reference\":\"ref_dup\",\"gateway_response\":\"declined\"}"));
        new ChargeFailedHandler(writer, paymentRepository).handle(e);
        verify(writer).upsertPaymentFailed("ref_dup", "m2", "declined");
        verify(writer, never()).recordFailure(anyString(), any());
    }
}
