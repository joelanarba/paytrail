package com.paytrail.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paytrail.common.ApiResponse;
import com.paytrail.document.ApiKey;
import com.paytrail.exception.UnauthorizedException;
import com.paytrail.modules.apikeys.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    private final ApiKeyService apiKeyService;
    private final String superApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(ApiKeyService apiKeyService, String superApiKey, ObjectMapper objectMapper) {
        this.apiKeyService = apiKeyService;
        this.superApiKey = superApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/webhooks/") || path.startsWith("/api/v1/dev/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader("X-Api-Key");
        try {
            if (key == null || key.isBlank()) {
                throw new UnauthorizedException("Missing API key");
            }
            if (superApiKey != null && superApiKey.equals(key)) {
                MerchantContext.set(null, true);
            } else {
                ApiKey apiKey = apiKeyService.validateKey(key);
                MerchantContext.set(apiKey.getMerchantId(), false);
            }
            chain.doFilter(request, response);
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized request: {}", e.getMessage());
            writeUnauthorized(response, e.getMessage());
        } finally {
            MerchantContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message, null));
    }
}
