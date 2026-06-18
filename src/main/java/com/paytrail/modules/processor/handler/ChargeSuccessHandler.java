package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import java.time.Instant;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class ChargeSuccessHandler implements EventHandler {
    private final ProjectionWriter writer;

    public ChargeSuccessHandler(ProjectionWriter writer) {
        this.writer = writer;
    }

    @Override
    public String eventType() {
        return "charge.success";
    }

    @Override
    public void handle(WebhookEvent event) {
        Document d = event.getParsedData() == null ? new Document() : event.getParsedData();
        long amount = ((Number) d.getOrDefault("amount", 0)).longValue();
        String currency = d.getString("currency");
        String channel = d.getString("channel");
        Document customer = d.get("customer", Document.class);
        String email = customer == null ? null : customer.getString("email");
        String name = customer == null ? null :
            ((nullToEmpty(customer.getString("first_name")) + " " + nullToEmpty(customer.getString("last_name"))).trim());
        Instant paidAt = Instant.now();
        writer.upsertPaymentSuccess(event.getReference(), event.getMerchantId(), amount, currency, email,
            name == null || name.isBlank() ? null : name, channel, paidAt);
        writer.recordSuccess(event.getMerchantId(), amount, paidAt);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
