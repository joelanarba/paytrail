package com.paytrail.modules.processor.handler;

import com.paytrail.document.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TransferSuccessHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(TransferSuccessHandler.class);

    @Override
    public String eventType() {
        return "transfer.success";
    }

    @Override
    public void handle(WebhookEvent event) {
        log.info("Transfer success: reference={}, merchantId={}", event.getReference(), event.getMerchantId());
    }
}
