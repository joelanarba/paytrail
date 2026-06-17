package com.paytrail.modules.webhook;

import com.paytrail.exception.InvalidSignatureException;
import com.paytrail.modules.webhook.controller.WebhookController;
import com.paytrail.modules.webhook.service.WebhookIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {
    @Autowired MockMvc mvc;
    @MockBean WebhookIngestionService service;

    @Test
    void validSignatureStoresAndReturns200() throws Exception {
        when(service.ingest(anyString(), eq("good"))).thenReturn("evt-123");
        mvc.perform(post("/api/v1/webhooks/paystack").content("{\"event\":\"charge.success\"}")
                .header("x-paystack-signature", "good").contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.eventId").value("evt-123"));
    }

    @Test
    void invalidSignatureReturns400() throws Exception {
        when(service.ingest(anyString(), eq("bad"))).thenThrow(new InvalidSignatureException("bad signature"));
        mvc.perform(post("/api/v1/webhooks/paystack").content("{}")
                .header("x-paystack-signature", "bad").contentType("application/json"))
            .andExpect(status().isBadRequest());
    }
}
