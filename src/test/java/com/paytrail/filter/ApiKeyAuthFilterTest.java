package com.paytrail.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paytrail.document.ApiKey;
import com.paytrail.exception.UnauthorizedException;
import com.paytrail.modules.apikeys.ApiKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiKeyAuthFilterTest {

    @RestController
    static class Probe {
        @GetMapping("/api/v1/events")
        String list() { return "ok"; }
    }

    private MockMvc mvc(ApiKeyService svc) {
        return MockMvcBuilders.standaloneSetup(new Probe())
            .addFilters(new ApiKeyAuthFilter(svc, "test-super-key", new ObjectMapper()))
            .build();
    }

    @Test
    void missingKeyReturns401() throws Exception {
        ApiKeyService svc = mock(ApiKeyService.class);
        mvc(svc).perform(get("/api/v1/events"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void wrongKeyReturns401() throws Exception {
        ApiKeyService svc = mock(ApiKeyService.class);
        when(svc.validateKey("bad")).thenThrow(new UnauthorizedException("Invalid API key"));
        mvc(svc).perform(get("/api/v1/events").header("X-Api-Key", "bad"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validKeyPasses() throws Exception {
        ApiKeyService svc = mock(ApiKeyService.class);
        ApiKey k = new ApiKey();
        k.setMerchantId("m1");
        k.setActive(true);
        when(svc.validateKey("good")).thenReturn(k);
        mvc(svc).perform(get("/api/v1/events").header("X-Api-Key", "good"))
            .andExpect(status().isOk());
    }

    @Test
    void superKeyPasses() throws Exception {
        ApiKeyService svc = mock(ApiKeyService.class);
        mvc(svc).perform(get("/api/v1/events").header("X-Api-Key", "test-super-key"))
            .andExpect(status().isOk());
        verify(svc, never()).validateKey(anyString());
    }
}
