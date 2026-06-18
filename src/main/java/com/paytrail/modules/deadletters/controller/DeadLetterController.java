package com.paytrail.modules.deadletters.controller;

import com.paytrail.common.ApiResponse;
import com.paytrail.common.PageResponse;
import com.paytrail.modules.deadletters.dto.DeadLetterResponse;
import com.paytrail.modules.deadletters.service.DeadLetterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dead-letters")
public class DeadLetterController {

    private final DeadLetterService service;

    public DeadLetterController(DeadLetterService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeadLetterResponse>>> list(
            @RequestParam(required = false) String merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Dead letters", service.list(merchantId, page, size)));
    }

    @PostMapping("/{eventId}/retry")
    public ResponseEntity<ApiResponse<String>> retry(@PathVariable String eventId) {
        service.retry(eventId);
        return ResponseEntity.ok(ApiResponse.ok("Event re-queued for processing", eventId));
    }
}
