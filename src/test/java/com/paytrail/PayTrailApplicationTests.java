package com.paytrail;

import com.paytrail.support.MockRedisTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(MockRedisTestConfig.class)
class PayTrailApplicationTests {
    @Test
    void contextLoads() {
    }
}
