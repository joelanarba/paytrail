package com.paytrail.bootstrap;

import com.paytrail.modules.apikeys.ApiKeyService;
import com.paytrail.repository.ApiKeyRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ApiKeySeederTest {
    @Test
    void seedsWhenEmpty() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKeyService svc = mock(ApiKeyService.class);
        when(repo.count()).thenReturn(0L);
        when(svc.generateKey("demo-merchant", "Auto-generated demo key")).thenReturn("demo-raw");
        new ApiKeySeeder(repo, svc).run(null);
        verify(svc).generateKey("demo-merchant", "Auto-generated demo key");
    }

    @Test
    void skipsWhenNotEmpty() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKeyService svc = mock(ApiKeyService.class);
        when(repo.count()).thenReturn(5L);
        new ApiKeySeeder(repo, svc).run(null);
        verify(svc, never()).generateKey(anyString(), anyString());
    }
}
