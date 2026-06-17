package com.paytrail.repository;

import com.paytrail.document.ApiKey;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ApiKeyRepository extends MongoRepository<ApiKey, String> {
    Optional<ApiKey> findByKeyHash(String keyHash);

    long count();
}
