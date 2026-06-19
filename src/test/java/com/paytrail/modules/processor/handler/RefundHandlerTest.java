package com.paytrail.modules.processor.handler;

import com.paytrail.document.PaymentProjection;
import com.paytrail.document.PaymentStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import com.paytrail.repository.PaymentProjectionRepository;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RefundHandlerTest {

    @Test
    void marksRefundedAndRecordsRefund() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        PaymentProjectionRepository paymentRepository = mock(PaymentProjectionRepository.class);
        when(paymentRepository.findByReference(anyString())).thenReturn(Optional.empty());
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m1"); e.setReference("ref_1");
        e.setParsedData(Document.parse("{\"reference\":\"ref_1\",\"amount\":5000}"));
        new RefundHandler(writer, paymentRepository).handle(e);
        verify(writer).upsertPaymentRefunded("ref_1");
        verify(writer).recordRefund("m1", 5000L);
    }

    @Test
    void doesNotIncrementCounterWhenAlreadyRefunded() {
        ProjectionWriter writer = mock(ProjectionWriter.class);
        PaymentProjectionRepository paymentRepository = mock(PaymentProjectionRepository.class);
        PaymentProjection existing = new PaymentProjection();
        existing.setReference("ref_dup");
        existing.setStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findByReference("ref_dup")).thenReturn(Optional.of(existing));
        WebhookEvent e = new WebhookEvent();
        e.setMerchantId("m2"); e.setReference("ref_dup");
        e.setParsedData(Document.parse("{\"reference\":\"ref_dup\",\"amount\":5000}"));
        new RefundHandler(writer, paymentRepository).handle(e);
        verify(writer).upsertPaymentRefunded("ref_dup");
        verify(writer, never()).recordRefund(anyString(), anyLong());
    }
}
