package com.paytrail.modules.events;

import com.paytrail.common.PageResponse;
import com.paytrail.document.EventStatus;
import com.paytrail.document.WebhookEvent;
import com.paytrail.exception.ResourceNotFoundException;
import com.paytrail.filter.MerchantContext;
import com.paytrail.modules.events.dto.EventSummary;
import com.paytrail.modules.events.service.EventQueryService;
import com.paytrail.repository.WebhookEventRepository;
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
class EventQueryServiceTest {
    @Autowired EventQueryService service;
    @Autowired WebhookEventRepository repo;

    @BeforeEach void seed() {
        repo.deleteAll();
        repo.save(evt("e1", "m1", EventStatus.PROCESSED));
        repo.save(evt("e2", "m2", EventStatus.RECEIVED));
    }
    @AfterEach void clear() { MerchantContext.clear(); }

    private WebhookEvent evt(String id, String merchant, EventStatus status) {
        WebhookEvent e = new WebhookEvent();
        e.setEventId(id); e.setMerchantId(merchant); e.setStatus(status);
        e.setPaystackEvent("charge.success"); e.setReference("ref-" + id); e.setReceivedAt(Instant.now());
        return e;
    }

    @Test
    void merchantSeesOnlyOwnEvents() {
        MerchantContext.set("m1", false);
        PageResponse<EventSummary> page = service.list(new EventQueryService.EventQueryFilters(), 0, 20);
        assertEquals(1, page.getTotalElements());
        assertEquals("e1", page.getContent().get(0).getEventId());
    }

    @Test
    void superKeySeesAll() {
        MerchantContext.set(null, true);
        assertEquals(2, service.list(new EventQueryService.EventQueryFilters(), 0, 20).getTotalElements());
    }

    @Test
    void merchantScopeAppliesEvenWithDateFilter() {
        MerchantContext.set("m1", false);
        EventQueryService.EventQueryFilters f = new EventQueryService.EventQueryFilters();
        f.from = Instant.now().minusSeconds(3600);
        f.to = Instant.now().plusSeconds(3600);
        PageResponse<EventSummary> page = service.list(f, 0, 20);
        assertEquals(1, page.getTotalElements(), "date filter must NOT bypass merchant scoping");
        assertEquals("e1", page.getContent().get(0).getEventId());
    }

    @Test
    void getByEventIdRejectsOtherMerchantWith404() {
        MerchantContext.set("m1", false);
        assertThrows(ResourceNotFoundException.class, () -> service.getByEventId("e2"));
    }
}
