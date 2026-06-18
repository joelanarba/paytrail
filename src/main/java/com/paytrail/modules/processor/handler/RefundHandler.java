package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RefundHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(RefundHandler.class);

    private final ProjectionWriter writer;

    public RefundHandler(ProjectionWriter writer) {
        this.writer = writer;
    }

    @Override
    public String eventType() {
        return "refund.processed";
    }

    @Override
    public void handle(WebhookEvent event) {
        log.debug("Handling refund.processed for reference {}", event.getReference());
        Document d = event.getParsedData() == null ? new Document() : event.getParsedData();
        Object raw = d.getOrDefault("amount", 0);
        long amount = raw instanceof Number n ? n.longValue() : 0L;
        writer.upsertPaymentRefunded(event.getReference());
        writer.recordRefund(event.getMerchantId(), amount);
    }
}
