package com.paytrail.modules.payments.service;

import com.paytrail.common.PageResponse;
import com.paytrail.document.PaymentProjection;
import com.paytrail.document.PaymentStatus;
import com.paytrail.exception.ResourceNotFoundException;
import com.paytrail.filter.MerchantContext;
import com.paytrail.modules.payments.dto.PaymentStatusResponse;
import com.paytrail.repository.PaymentProjectionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class PaymentQueryService {

    public static class PaymentQueryFilters {
        public PaymentStatus status;
        public String channel;
        public String merchantId;
        public Instant from;
        public Instant to;
    }

    private final PaymentProjectionRepository paymentRepository;
    private final MongoTemplate mongo;

    public PaymentQueryService(PaymentProjectionRepository paymentRepository, MongoTemplate mongo) {
        this.paymentRepository = paymentRepository;
        this.mongo = mongo;
    }

    /** Retrieves the current payment projection for the given Paystack reference, enforcing merchant scoping. */
    public PaymentStatusResponse getByReference(String reference) {
        PaymentProjection p = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!MerchantContext.isSuperKey() && !p.getMerchantId().equals(MerchantContext.getMerchantId())) {
            throw new ResourceNotFoundException("Payment not found");
        }
        return toResponse(p);
    }

    /** Returns a paginated list of payment projections filtered by the supplied criteria and scoped to the authenticated merchant. */
    public PageResponse<PaymentStatusResponse> list(PaymentQueryFilters f, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = (size < 1) ? 20 : size;
        page = safePage;
        size = safeSize;
        List<Criteria> parts = new ArrayList<>();
        if (!MerchantContext.isSuperKey()) {
            parts.add(Criteria.where("merchantId").is(MerchantContext.getMerchantId()));
        } else if (f.merchantId != null) {
            parts.add(Criteria.where("merchantId").is(f.merchantId));
        }
        if (f.status != null) {
            parts.add(Criteria.where("status").is(f.status));
        }
        if (f.channel != null) {
            parts.add(Criteria.where("channel").is(f.channel));
        }
        if (f.from != null || f.to != null) {
            Criteria t = Criteria.where("createdAt");
            if (f.from != null) t.gte(f.from);
            if (f.to != null) t.lte(f.to);
            parts.add(t);
        }
        Criteria c = parts.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(parts.toArray(new Criteria[0]));
        Query query = new Query(c);
        long total = mongo.count(query, PaymentProjection.class);
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<PaymentStatusResponse> content = mongo.find(query, PaymentProjection.class)
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(content, page, size, total);
    }

    private PaymentStatusResponse toResponse(PaymentProjection p) {
        PaymentStatusResponse r = new PaymentStatusResponse();
        r.setReference(p.getReference());
        r.setMerchantId(p.getMerchantId());
        r.setAmount(p.getAmount());
        r.setCurrency(p.getCurrency());
        r.setStatus(p.getStatus());
        r.setCustomerEmail(p.getCustomerEmail());
        r.setCustomerName(p.getCustomerName());
        r.setChannel(p.getChannel());
        r.setPaidAt(p.getPaidAt());
        r.setFailureReason(p.getFailureReason());
        r.setRefundedAt(p.getRefundedAt());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
