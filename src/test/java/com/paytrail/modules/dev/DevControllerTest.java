package com.paytrail.modules.dev;

import com.paytrail.modules.apikeys.ApiKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DevController.class)
@TestPropertySource(properties = "paytrail.super-api-key=test-super-key")
class DevControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ApiKeyService apiKeyService;

    @Test
    void createsKeyWithValidSuperKey() throws Exception {
        when(apiKeyService.generateKey("m1", "demo")).thenReturn("raw-key");
        mvc.perform(post("/api/v1/dev/api-keys").header("X-Super-Key", "test-super-key")
                .contentType("application/json").content("{\"merchantId\":\"m1\",\"description\":\"demo\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.apiKey").value("raw-key"));
    }

    @Test
    void rejectsWrongSuperKey() throws Exception {
        mvc.perform(post("/api/v1/dev/api-keys").header("X-Super-Key", "wrong")
                .contentType("application/json").content("{\"merchantId\":\"m1\",\"description\":\"demo\"}"))
            .andExpect(status().isUnauthorized());
    }
}
