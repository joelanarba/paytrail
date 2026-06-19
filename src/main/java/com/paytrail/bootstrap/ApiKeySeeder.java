package com.paytrail.bootstrap;

import com.paytrail.modules.apikeys.ApiKeyService;
import com.paytrail.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ApiKeySeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ApiKeySeeder.class);
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;

    public ApiKeySeeder(ApiKeyRepository apiKeyRepository, ApiKeyService apiKeyService) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyService = apiKeyService;
    }

    /** Seeds a demo API key for the 'demo-merchant' on first startup if no keys exist. */
    @Override
    public void run(ApplicationArguments args) {
        if (apiKeyRepository.count() == 0) {
            String raw = apiKeyService.generateKey("demo-merchant", "Auto-generated demo key");
            log.info("[PAYTRAIL STARTUP] Demo API key for merchant 'demo-merchant': {}", raw);
        }
    }
}
