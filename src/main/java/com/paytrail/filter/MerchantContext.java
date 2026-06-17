package com.paytrail.filter;

public final class MerchantContext {
    private static final ThreadLocal<String> MERCHANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPER = ThreadLocal.withInitial(() -> false);

    private MerchantContext() { }

    public static void set(String merchantId, boolean superKey) {
        MERCHANT.set(merchantId);
        SUPER.set(superKey);
    }

    public static String getMerchantId() { return MERCHANT.get(); }

    public static boolean isSuperKey() { return Boolean.TRUE.equals(SUPER.get()); }

    public static void clear() {
        MERCHANT.remove();
        SUPER.remove();
    }
}
