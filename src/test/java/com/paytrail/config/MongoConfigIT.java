package com.paytrail.config;

import com.paytrail.document.PaymentProjection;
import com.paytrail.support.MockRedisTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(MockRedisTestConfig.class)
class MongoConfigIT {
    @Autowired MongoTemplate mongoTemplate;

    @Test
    void createsUniqueReferenceIndexOnPaymentProjections() {
        boolean hasUniqueReference = mongoTemplate.indexOps(PaymentProjection.class).getIndexInfo().stream()
            .anyMatch(i -> i.isUnique() && i.getIndexFields().stream().anyMatch(f -> f.getKey().equals("reference")));
        assertTrue(hasUniqueReference);
    }
}
