package com.paytrail.modules.deadletters;

import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.exception.ResourceNotFoundException;
import com.paytrail.filter.MerchantContext;
import com.paytrail.modules.deadletters.service.DeadLetterService;
import com.paytrail.repository.WebhookEventRepository;
import com.paytrail.support.MockRedisTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Import(MockRedisTestConfig.class)
class DeadLetterServiceTest {
    @Autowired DeadLetterService service;
    @Autowired WebhookEventRepository repo;

    @BeforeEach void seed() {
        repo.deleteAll();
        WebhookEvent e = new WebhookEvent();
        e.setEventId("d1"); e.setMerchantId("m1"); e.setStatus(EventStatus.DEAD_LETTER);
        e.setRetryCount(3); e.setFailureReason("boom"); e.setReference("ref-d1"); e.setPaystackEvent("charge.success");
        repo.save(e);
    }
    @AfterEach void clear() { MerchantContext.clear(); }

    @Test
    void retryResetsEvent() {
        MerchantContext.set("m1", false);
        service.retry("d1");
        WebhookEvent e = repo.findByEventId("d1").orElseThrow();
        assertEquals(EventStatus.RECEIVED, e.getStatus());
        assertEquals(0, e.getRetryCount());
        assertNull(e.getFailureReason());
    }

    @Test
    void cannotRetryOtherMerchantEvent() {
        MerchantContext.set("m2", false);
        assertThrows(ResourceNotFoundException.class, () -> service.retry("d1"));
    }
}
