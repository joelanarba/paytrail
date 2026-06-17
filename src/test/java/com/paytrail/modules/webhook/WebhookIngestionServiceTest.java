package com.paytrail.modules.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.exception.InvalidSignatureException;
import com.paytrail.modules.webhook.service.WebhookIngestionService;
import com.paytrail.repository.WebhookEventRepository;
import com.paytrail.util.EventParser;
import com.paytrail.util.HmacUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookIngestionServiceTest {
    private static final String SECRET = "test_secret_key_for_demo";
    private static final String BODY =
        "{\"event\":\"charge.success\",\"data\":{\"reference\":\"ref_1\",\"metadata\":{\"merchant_id\":\"m1\"}}}";

    private final WebhookEventRepository repo = mock(WebhookEventRepository.class);
    private final EventParser parser = new EventParser(new ObjectMapper());
    private final WebhookIngestionService service = new WebhookIngestionService(repo, parser, SECRET);

    @Test
    void invalidSignatureThrowsAndDoesNotSave() {
        assertThrows(InvalidSignatureException.class, () -> service.ingest(BODY, "deadbeef"));
        verify(repo, never()).save(any());
    }

    @Test
    void validSignatureStoresReceivedEvent() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        String sig = HmacUtil.computeHmacSha512(BODY, SECRET);
        String eventId = service.ingest(BODY, sig);
        assertNotNull(eventId);
        ArgumentCaptor<WebhookEvent> cap = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(repo).save(cap.capture());
        WebhookEvent saved = cap.getValue();
        assertEquals(EventStatus.RECEIVED, saved.getStatus());
        assertEquals("m1", saved.getMerchantId());
        assertEquals("charge.success", saved.getPaystackEvent());
        assertEquals(eventId, saved.getEventId());
    }
}
