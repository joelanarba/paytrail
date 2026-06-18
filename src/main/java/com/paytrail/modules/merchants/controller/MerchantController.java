package com.paytrail.modules.merchants.controller;

import com.paytrail.common.ApiResponse;
import com.paytrail.modules.merchants.dto.MerchantSummaryResponse;
import com.paytrail.modules.merchants.service.MerchantSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantSummaryService merchantSummaryService;

    public MerchantController(MerchantSummaryService merchantSummaryService) {
        this.merchantSummaryService = merchantSummaryService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MerchantSummaryResponse>> getCurrentSummary() {
        return ResponseEntity.ok(
                ApiResponse.ok("Merchant summary", merchantSummaryService.getCurrentMerchantSummary()));
    }

    @GetMapping("/{merchantId}/summary")
    public ResponseEntity<ApiResponse<MerchantSummaryResponse>> getMerchantSummary(
            @PathVariable String merchantId) {
        return ResponseEntity.ok(
                ApiResponse.ok("Merchant summary", merchantSummaryService.getMerchantSummary(merchantId)));
    }
}
