package com.paytrail.modules.dev;

import com.paytrail.common.ApiResponse;
import com.paytrail.exception.UnauthorizedException;
import com.paytrail.modules.apikeys.ApiKeyService;
import com.paytrail.modules.dev.dto.CreateApiKeyRequest;
import com.paytrail.modules.dev.dto.CreateApiKeyResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dev")
public class DevController {
    private final ApiKeyService apiKeyService;
    private final String superApiKey;

    public DevController(
            ApiKeyService apiKeyService,
            @Value("${paytrail.super-api-key}") String superApiKey) {
        this.apiKeyService = apiKeyService;
        this.superApiKey = superApiKey;
    }

    @PostMapping("/api-keys")
    public ResponseEntity<ApiResponse<CreateApiKeyResponse>> create(
            @RequestHeader(value = "X-Super-Key", required = false) String superKey,
            @Valid @RequestBody CreateApiKeyRequest request) {
        if (superApiKey == null || !superApiKey.equals(superKey)) {
            throw new UnauthorizedException("Invalid super key");
        }
        String raw = apiKeyService.generateKey(request.getMerchantId(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.ok("API key created", new CreateApiKeyResponse(raw)));
    }
}
