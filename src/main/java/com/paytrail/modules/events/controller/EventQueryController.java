package com.paytrail.modules.events.controller;

import com.paytrail.common.ApiResponse;
import com.paytrail.common.PageResponse;
import com.paytrail.document.EventStatus;
import com.paytrail.modules.events.dto.EventDetail;
import com.paytrail.modules.events.dto.EventSummary;
import com.paytrail.modules.events.service.EventQueryService;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventQueryController {

    private final EventQueryService service;

    public EventQueryController(EventQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventSummary>>> list(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String paystackEvent,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        EventQueryService.EventQueryFilters f = new EventQueryService.EventQueryFilters();
        f.status = status;
        f.paystackEvent = paystackEvent;
        f.merchantId = merchantId;
        f.from = from;
        f.to = to;
        return ResponseEntity.ok(ApiResponse.ok("Events", service.list(f, page, size)));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDetail>> get(@PathVariable String eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Event", service.getByEventId(eventId)));
    }
}
