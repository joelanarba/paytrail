package com.paytrail.modules.processor.service;

import com.paytrail.document.PaymentProjection;
import com.paytrail.document.PaymentStatus;
import com.paytrail.repository.PaymentProjectionRepository;
import com.paytrail.support.MockRedisTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(MockRedisTestConfig.class)
class ProjectionWriterTest {
    @Autowired ProjectionWriter writer;
    @Autowired PaymentProjectionRepository payments;

    @BeforeEach
    void clearProjections() {
        payments.deleteAll();
    }

    @Test
    void upsertCreatesThenUpdatesByReference() {
        writer.upsertPaymentSuccess("ref_x", "m1", 5000L, "NGN", "a@b.com", "A B", "card", Instant.now());
        writer.upsertPaymentSuccess("ref_x", "m1", 5000L, "NGN", "a@b.com", "A B", "card", Instant.now());
        var all = payments.findAll();
        assertEquals(1, all.size());
        PaymentProjection p = all.get(0);
        assertEquals(PaymentStatus.SUCCESS, p.getStatus());
        assertEquals(5000L, p.getAmount());
    }
}
