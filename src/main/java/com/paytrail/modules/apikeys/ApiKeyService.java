package com.paytrail.modules.apikeys;

import com.paytrail.document.ApiKey;
import com.paytrail.exception.UnauthorizedException;
import com.paytrail.repository.ApiKeyRepository;
import com.paytrail.util.ApiKeyUtil;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {
    private final ApiKeyRepository repository;

    public ApiKeyService(ApiKeyRepository repository) { this.repository = repository; }

    public String generateKey(String merchantId, String description) {
        String raw = ApiKeyUtil.generateRawKey();
        ApiKey key = new ApiKey();
        key.setKeyHash(ApiKeyUtil.sha256Hex(raw));
        key.setMerchantId(merchantId);
        key.setDescription(description);
        key.setActive(true);
        key.setCreatedAt(Instant.now());
        repository.save(key);
        return raw;
    }

    public ApiKey validateKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) throw new UnauthorizedException("Missing API key");
        ApiKey key = repository.findByKeyHash(ApiKeyUtil.sha256Hex(rawKey))
            .orElseThrow(() -> new UnauthorizedException("Invalid API key"));
        if (!key.isActive()) throw new UnauthorizedException("Inactive API key");
        key.setLastUsedAt(Instant.now());
        return repository.save(key);
    }
}
