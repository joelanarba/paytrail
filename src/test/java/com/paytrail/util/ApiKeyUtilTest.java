package com.paytrail.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiKeyUtilTest {
    @Test
    void generatesDistinctKeys() {
        assertNotEquals(ApiKeyUtil.generateRawKey(), ApiKeyUtil.generateRawKey());
    }
    @Test
    void hashIsStableAndHex() {
        String h = ApiKeyUtil.sha256Hex("abc");
        assertEquals(64, h.length());
        assertEquals(h, ApiKeyUtil.sha256Hex("abc"));
    }
}
