package com.paytrail.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HmacUtilTest {
    private static final String SECRET = "test_secret_key_for_demo";
    private static final String PAYLOAD = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"ref_1\"}}";

    @Test
    void computesStableLowercaseHex() {
        String sig = HmacUtil.computeHmacSha512(PAYLOAD, SECRET);
        assertEquals(128, sig.length());
        assertEquals(sig.toLowerCase(), sig);
        assertEquals(sig, HmacUtil.computeHmacSha512(PAYLOAD, SECRET));
    }

    @Test
    void verifiesCorrectSignature() {
        String sig = HmacUtil.computeHmacSha512(PAYLOAD, SECRET);
        assertTrue(HmacUtil.verifySignature(PAYLOAD, SECRET, sig));
    }

    @Test
    void rejectsTamperedPayload() {
        String sig = HmacUtil.computeHmacSha512(PAYLOAD, SECRET);
        assertFalse(HmacUtil.verifySignature(PAYLOAD + "x", SECRET, sig));
    }

    @Test
    void rejectsWrongSecret() {
        String sig = HmacUtil.computeHmacSha512(PAYLOAD, SECRET);
        assertFalse(HmacUtil.verifySignature(PAYLOAD, "other_secret", sig));
    }

    @Test
    void rejectsNullSignature() {
        assertFalse(HmacUtil.verifySignature(PAYLOAD, SECRET, null));
    }
}
