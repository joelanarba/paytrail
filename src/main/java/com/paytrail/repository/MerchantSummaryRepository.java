package com.paytrail.repository;

import com.paytrail.document.MerchantSummary;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MerchantSummaryRepository extends MongoRepository<MerchantSummary, String> {
    Optional<MerchantSummary> findByMerchantId(String merchantId);
}
