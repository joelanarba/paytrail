package com.paytrail.modules.webhook.controller;

import com.paytrail.common.ApiResponse;
import com.paytrail.modules.webhook.dto.WebhookAck;
import com.paytrail.modules.webhook.service.WebhookIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookIngestionService ingestionService;

    public WebhookController(WebhookIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/paystack")
    public ResponseEntity<ApiResponse<WebhookAck>> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        String eventId = ingestionService.ingest(rawBody, signature);
        return ResponseEntity.ok(ApiResponse.ok("Event received", new WebhookAck(eventId)));
    }
}
