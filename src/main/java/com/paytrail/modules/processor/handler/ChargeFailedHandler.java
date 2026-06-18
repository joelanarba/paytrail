package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import java.time.Instant;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChargeFailedHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(ChargeFailedHandler.class);

    private final ProjectionWriter writer;

    public ChargeFailedHandler(ProjectionWriter writer) {
        this.writer = writer;
    }

    @Override
    public String eventType() {
        return "charge.failed";
    }

    @Override
    public void handle(WebhookEvent event) {
        log.debug("Handling charge.failed for reference {}", event.getReference());
        Document d = event.getParsedData() == null ? new Document() : event.getParsedData();
        String reason = d.getString("gateway_response");
        writer.upsertPaymentFailed(event.getReference(), event.getMerchantId(), reason);
        writer.recordFailure(event.getMerchantId(), Instant.now());
    }
}
