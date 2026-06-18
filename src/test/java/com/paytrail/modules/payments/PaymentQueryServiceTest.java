package com.paytrail.modules.payments;

import com.paytrail.common.PageResponse;
import com.paytrail.document.PaymentProjection;
import com.paytrail.document.PaymentStatus;
import com.paytrail.exception.ResourceNotFoundException;
import com.paytrail.filter.MerchantContext;
import com.paytrail.modules.payments.dto.PaymentStatusResponse;
import com.paytrail.modules.payments.service.PaymentQueryService;
import com.paytrail.repository.PaymentProjectionRepository;
import com.paytrail.support.MockRedisTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(MockRedisTestConfig.class)
class PaymentQueryServiceTest {

    @Autowired PaymentQueryService service;
    @Autowired PaymentProjectionRepository repo;

    @BeforeEach
    void seed() {
        repo.deleteAll();
        PaymentProjection p = new PaymentProjection();
        p.setReference("ref1");
        p.setMerchantId("m1");
        p.setStatus(PaymentStatus.SUCCESS);
        p.setAmount(5000L);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        repo.save(p);

        PaymentProjection p2 = new PaymentProjection();
        p2.setReference("ref2");
        p2.setMerchantId("m2");
        p2.setStatus(PaymentStatus.FAILED);
        p2.setAmount(3000L);
        p2.setCreatedAt(Instant.now());
        p2.setUpdatedAt(Instant.now());
        repo.save(p2);
    }

    @AfterEach
    void clear() {
        MerchantContext.clear();
    }

    @Test
    void ownerCanRead() {
        MerchantContext.set("m1", false);
        assertEquals(PaymentStatus.SUCCESS, service.getByReference("ref1").getStatus());
    }

    @Test
    void otherMerchantGets404() {
        MerchantContext.set("m2", false);
        assertThrows(ResourceNotFoundException.class, () -> service.getByReference("ref1"));
    }

    @Test
    void listScopesToCallerMerchant() {
        MerchantContext.set("m1", false);
        PaymentQueryService.PaymentQueryFilters f = new PaymentQueryService.PaymentQueryFilters();
        PageResponse<PaymentStatusResponse> page = service.list(f, 0, 20);
        assertEquals(1, page.getTotalElements(), "non-super caller must only see their own payments");
        assertEquals("ref1", page.getContent().get(0).getReference());
    }

    @Test
    void superKeySeesAll() {
        MerchantContext.set(null, true);
        PaymentQueryService.PaymentQueryFilters f = new PaymentQueryService.PaymentQueryFilters();
        PageResponse<PaymentStatusResponse> page = service.list(f, 0, 20);
        assertEquals(2, page.getTotalElements(), "super key must see all payments");
    }
}
