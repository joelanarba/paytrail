package com.paytrail.modules.processor.service;

import com.paytrail.document.MerchantSummary;
import com.paytrail.document.PaymentProjection;
import com.paytrail.document.PaymentStatus;
import java.time.Instant;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class ProjectionWriter {

    private final MongoTemplate mongo;

    public ProjectionWriter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private Query byReference(String reference) {
        return new Query(Criteria.where("reference").is(reference));
    }

    private Query byMerchant(String merchantId) {
        return new Query(Criteria.where("merchantId").is(merchantId));
    }

    public void upsertPaymentSuccess(String reference, String merchantId, long amount, String currency,
                                     String email, String name, String channel, Instant paidAt) {
        Instant now = Instant.now();
        Update u = new Update()
            .set("merchantId", merchantId)
            .set("amount", amount)
            .set("currency", currency)
            .set("status", PaymentStatus.SUCCESS)
            .set("customerEmail", email)
            .set("customerName", name)
            .set("channel", channel)
            .set("paidAt", paidAt)
            .set("updatedAt", now)
            .setOnInsert("createdAt", now);
        mongo.upsert(byReference(reference), u, PaymentProjection.class);
    }

    public void upsertPaymentFailed(String reference, String merchantId, String failureReason) {
        Instant now = Instant.now();
        Update u = new Update()
            .set("merchantId", merchantId)
            .set("status", PaymentStatus.FAILED)
            .set("failureReason", failureReason)
            .set("updatedAt", now)
            .setOnInsert("createdAt", now);
        mongo.upsert(byReference(reference), u, PaymentProjection.class);
    }

    public void upsertPaymentRefunded(String reference) {
        Instant now = Instant.now();
        Update u = new Update()
            .set("status", PaymentStatus.REFUNDED)
            .set("refundedAt", now)
            .set("updatedAt", now);
        mongo.upsert(byReference(reference), u, PaymentProjection.class);
    }

    public void recordSuccess(String merchantId, long amount, Instant when) {
        Update u = new Update()
            .inc("totalTransactions", 1)
            .inc("successfulTransactions", 1)
            .inc("totalRevenue", amount)
            .set("lastTransactionAt", when)
            .set("updatedAt", Instant.now());
        mongo.upsert(byMerchant(merchantId), u, MerchantSummary.class);
    }

    public void recordFailure(String merchantId, Instant when) {
        Update u = new Update()
            .inc("totalTransactions", 1)
            .inc("failedTransactions", 1)
            .set("lastTransactionAt", when)
            .set("updatedAt", Instant.now());
        mongo.upsert(byMerchant(merchantId), u, MerchantSummary.class);
    }

    public void recordRefund(String merchantId, long refundedAmount) {
        Update u = new Update()
            .inc("totalRefunded", refundedAmount)
            .set("updatedAt", Instant.now());
        mongo.upsert(byMerchant(merchantId), u, MerchantSummary.class);
    }
}
