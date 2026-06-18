package com.paytrail.modules.payments.controller;

import com.paytrail.common.ApiResponse;
import com.paytrail.common.PageResponse;
import com.paytrail.document.PaymentStatus;
import com.paytrail.modules.payments.dto.PaymentStatusResponse;
import com.paytrail.modules.payments.service.PaymentQueryService;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentQueryService service;

    public PaymentController(PaymentQueryService service) {
        this.service = service;
    }

    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getByReference(
            @PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.ok("Payment", service.getByReference(reference)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentStatusResponse>>> list(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaymentQueryService.PaymentQueryFilters f = new PaymentQueryService.PaymentQueryFilters();
        f.status = status;
        f.channel = channel;
        f.merchantId = merchantId;
        f.from = from;
        f.to = to;
        return ResponseEntity.ok(ApiResponse.ok("Payments", service.list(f, page, size)));
    }
}
