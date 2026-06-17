package com.paytrail.filter;

import com.paytrail.support.MockRedisTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MockRedisTestConfig.class)
class ApiKeyAuthFilterRoutingTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void nestedApiPathWithoutKeyIsRejected() {
        // No controllers exist yet for these paths. If the filter runs, we get 401.
        // If the filter is bypassed, the request reaches the dispatcher and returns 404.
        ResponseEntity<String> r = rest.getForEntity("/api/v1/events/some-id", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode(),
            "nested /api/v1/** path must be auth-filtered, not bypassed");
    }

    @Test
    void topLevelApiPathWithoutKeyIsRejected() {
        ResponseEntity<String> r = rest.getForEntity("/api/v1/events", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
    }

    @Test
    void webhookPathBypassesAuth() {
        ResponseEntity<String> r = rest.getForEntity("/api/v1/webhooks/ping", String.class);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode(),
            "excluded webhook path with no handler should be 404, not 401 or 500");
    }
}
