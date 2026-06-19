package com.paytrail.modules.merchants.service;

import com.paytrail.document.MerchantSummary;
import com.paytrail.exception.ForbiddenException;
import com.paytrail.filter.MerchantContext;
import com.paytrail.modules.merchants.dto.MerchantSummaryResponse;
import com.paytrail.repository.MerchantSummaryRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MerchantSummaryService {

    private final MerchantSummaryRepository merchantSummaryRepository;

    public MerchantSummaryService(MerchantSummaryRepository merchantSummaryRepository) {
        this.merchantSummaryRepository = merchantSummaryRepository;
    }

    /** Returns the aggregated payment summary for the merchant bound to the current request context. */
    public MerchantSummaryResponse getCurrentMerchantSummary() {
        String merchantId = MerchantContext.getMerchantId();
        Optional<MerchantSummary> found = merchantSummaryRepository.findByMerchantId(merchantId);
        return found.map(this::toResponse).orElse(MerchantSummaryResponse.zeroed(merchantId));
    }

    /** Returns the aggregated payment summary for any merchant by ID; requires a super key. */
    public MerchantSummaryResponse getMerchantSummary(String merchantId) {
        if (!MerchantContext.isSuperKey()) {
            throw new ForbiddenException("Access denied: super key required");
        }
        Optional<MerchantSummary> found = merchantSummaryRepository.findByMerchantId(merchantId);
        return found.map(this::toResponse).orElse(MerchantSummaryResponse.zeroed(merchantId));
    }

    private MerchantSummaryResponse toResponse(MerchantSummary s) {
        MerchantSummaryResponse r = new MerchantSummaryResponse();
        r.setMerchantId(s.getMerchantId());
        r.setTotalTransactions(s.getTotalTransactions());
        r.setSuccessfulTransactions(s.getSuccessfulTransactions());
        r.setFailedTransactions(s.getFailedTransactions());
        r.setTotalRevenue(s.getTotalRevenue());
        r.setTotalRefunded(s.getTotalRefunded());
        r.setLastTransactionAt(s.getLastTransactionAt());
        r.setUpdatedAt(s.getUpdatedAt());
        return r;
    }
}
