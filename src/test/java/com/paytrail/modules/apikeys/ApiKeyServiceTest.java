package com.paytrail.modules.apikeys;

import com.paytrail.document.ApiKey;
import com.paytrail.exception.UnauthorizedException;
import com.paytrail.repository.ApiKeyRepository;
import com.paytrail.util.ApiKeyUtil;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyServiceTest {
    private final ApiKeyRepository repo = mock(ApiKeyRepository.class);
    private final ApiKeyService service = new ApiKeyService(repo);

    @Test
    void generateStoresHashAndReturnsRawOnce() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        String raw = service.generateKey("m1", "desc");
        assertNotNull(raw);
        verify(repo).save(argThat(k -> k.getKeyHash().equals(ApiKeyUtil.sha256Hex(raw))
            && k.getMerchantId().equals("m1") && k.isActive()));
    }

    @Test
    void validateReturnsKeyAndUpdatesLastUsed() {
        ApiKey k = new ApiKey();
        k.setMerchantId("m1"); k.setActive(true); k.setKeyHash(ApiKeyUtil.sha256Hex("raw"));
        when(repo.findByKeyHash(ApiKeyUtil.sha256Hex("raw"))).thenReturn(Optional.of(k));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        ApiKey result = service.validateKey("raw");
        assertEquals("m1", result.getMerchantId());
        assertNotNull(result.getLastUsedAt());
    }

    @Test
    void validateRejectsUnknownKey() {
        when(repo.findByKeyHash(anyString())).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> service.validateKey("nope"));
    }

    @Test
    void validateRejectsInactiveKey() {
        ApiKey k = new ApiKey();
        k.setActive(false); k.setKeyHash(ApiKeyUtil.sha256Hex("raw"));
        when(repo.findByKeyHash(ApiKeyUtil.sha256Hex("raw"))).thenReturn(Optional.of(k));
        assertThrows(UnauthorizedException.class, () -> service.validateKey("raw"));
    }
}
