package com.paytrail.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paytrail.filter.ApiKeyAuthFilter;
import com.paytrail.modules.apikeys.ApiKeyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSecurityConfig {

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(
            ApiKeyService apiKeyService,
            @Value("${paytrail.super-api-key}") String superApiKey,
            ObjectMapper objectMapper) {
        FilterRegistrationBean<ApiKeyAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new ApiKeyAuthFilter(apiKeyService, superApiKey, objectMapper));
        reg.addUrlPatterns("/api/v1/*");
        reg.setOrder(1);
        return reg;
    }
}
