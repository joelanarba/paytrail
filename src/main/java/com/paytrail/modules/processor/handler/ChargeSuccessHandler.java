package com.paytrail.modules.processor.handler;

import com.paytrail.document.PaymentStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.modules.processor.service.ProjectionWriter;
import com.paytrail.repository.PaymentProjectionRepository;
import java.time.Instant;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChargeSuccessHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(ChargeSuccessHandler.class);

    private final ProjectionWriter writer;
    private final PaymentProjectionRepository paymentRepository;

    public ChargeSuccessHandler(ProjectionWriter writer, PaymentProjectionRepository paymentRepository) {
        this.writer = writer;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public String eventType() {
        return "charge.success";
    }

    @Override
    public void handle(WebhookEvent event) {
        log.debug("Handling charge.success for reference {}", event.getReference());
        Document d = event.getParsedData() == null ? new Document() : event.getParsedData();
        Object rawAmount = d.getOrDefault("amount", 0);
        long amount = rawAmount instanceof Number n ? n.longValue() : 0L;
        String currency = d.getString("currency");
        String channel = d.getString("channel");
        Document customer = d.get("customer", Document.class);
        String email = customer == null ? null : customer.getString("email");
        String name = customer == null ? null :
            ((nullToEmpty(customer.getString("first_name")) + " " + nullToEmpty(customer.getString("last_name"))).trim());
        Instant paidAt = parsePaidAt(d.getString("paid_at"));
        boolean firstSuccess = paymentRepository.findByReference(event.getReference())
                .map(p -> p.getStatus() != PaymentStatus.SUCCESS)
                .orElse(true);
        writer.upsertPaymentSuccess(event.getReference(), event.getMerchantId(), amount, currency, email,
            name == null || name.isBlank() ? null : name, channel, paidAt);
        if (firstSuccess) {
            writer.recordSuccess(event.getMerchantId(), amount, paidAt);
        }
    }

    private static Instant parsePaidAt(String s) {
        if (s == null || s.isBlank()) return Instant.now();
        try {
            return java.time.OffsetDateTime.parse(s).toInstant();
        } catch (Exception e1) {
            try {
                return Instant.parse(s);
            } catch (Exception e2) {
                return Instant.now();
            }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
