package com.paytrail.repository;

import com.paytrail.document.PaymentProjection;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentProjectionRepository extends MongoRepository<PaymentProjection, String> {
    Optional<PaymentProjection> findByReference(String reference);

    Optional<PaymentProjection> findByReferenceAndMerchantId(String reference, String merchantId);
}
