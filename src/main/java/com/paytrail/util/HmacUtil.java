package com.paytrail.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacUtil {
    private static final String ALGO = "HmacSHA512";
    private HmacUtil() { }

    public static String computeHmacSha512(String payload, String secret) {
        return bytesToHex(computeRaw(payload, secret));
    }

    private static byte[] computeRaw(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC", e);
        }
    }

    private static String bytesToHex(byte[] raw) {
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    public static boolean verifySignature(String payload, String secret, String signature) {
        if (signature == null) return false;
        byte[] received = hexToBytes(signature);
        if (received == null) return false;
        return MessageDigest.isEqual(computeRaw(payload, secret), received);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) return null;
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
