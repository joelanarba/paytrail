package com.paytrail.config;

import com.paytrail.document.ApiKey;
import com.paytrail.document.MerchantSummary;
import com.paytrail.document.PaymentProjection;
import com.paytrail.document.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class MongoConfig {
    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);
    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        mongoTemplate.indexOps(WebhookEvent.class).ensureIndex(
            new Index().on("reference", Sort.Direction.ASC).on("paystackEvent", Sort.Direction.ASC).named("reference_event"));
        mongoTemplate.indexOps(WebhookEvent.class).ensureIndex(
            new Index().on("status", Sort.Direction.ASC).named("status"));
        mongoTemplate.indexOps(WebhookEvent.class).ensureIndex(
            new Index().on("merchantId", Sort.Direction.ASC).named("merchantId"));
        mongoTemplate.indexOps(PaymentProjection.class).ensureIndex(
            new Index().on("reference", Sort.Direction.ASC).unique().named("reference_unique"));
        mongoTemplate.indexOps(PaymentProjection.class).ensureIndex(
            new Index().on("merchantId", Sort.Direction.ASC).named("merchantId"));
        mongoTemplate.indexOps(MerchantSummary.class).ensureIndex(
            new Index().on("merchantId", Sort.Direction.ASC).unique().named("merchantId_unique"));
        mongoTemplate.indexOps(ApiKey.class).ensureIndex(
            new Index().on("keyHash", Sort.Direction.ASC).unique().named("keyHash_unique"));
        log.info("MongoDB indexes ensured for webhook_events, payment_projections, merchant_summaries, api_keys");
    }
}
