package com.paytrail.modules.merchants.dto;

import java.time.Instant;

public class MerchantSummaryResponse {

    private String merchantId;
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long totalRevenue;
    private long totalRefunded;
    private Instant lastTransactionAt;
    private Instant updatedAt;

    public MerchantSummaryResponse() { }

    public static MerchantSummaryResponse zeroed(String merchantId) {
        MerchantSummaryResponse r = new MerchantSummaryResponse();
        r.merchantId = merchantId;
        r.totalTransactions = 0L;
        r.successfulTransactions = 0L;
        r.failedTransactions = 0L;
        r.totalRevenue = 0L;
        r.totalRefunded = 0L;
        r.lastTransactionAt = null;
        r.updatedAt = null;
        return r;
    }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }

    public long getSuccessfulTransactions() { return successfulTransactions; }
    public void setSuccessfulTransactions(long successfulTransactions) { this.successfulTransactions = successfulTransactions; }

    public long getFailedTransactions() { return failedTransactions; }
    public void setFailedTransactions(long failedTransactions) { this.failedTransactions = failedTransactions; }

    public long getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(long totalRevenue) { this.totalRevenue = totalRevenue; }

    public long getTotalRefunded() { return totalRefunded; }
    public void setTotalRefunded(long totalRefunded) { this.totalRefunded = totalRefunded; }

    public Instant getLastTransactionAt() { return lastTransactionAt; }
    public void setLastTransactionAt(Instant lastTransactionAt) { this.lastTransactionAt = lastTransactionAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
