package com.paytrail.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@org.springframework.data.mongodb.core.mapping.Document(collection = "merchant_summaries")
public class MerchantSummary {

    @Id private String id;
    @Indexed(unique = true) private String merchantId;
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long totalRevenue;
    private long totalRefunded;
    private Instant lastTransactionAt;
    private Instant updatedAt;

    public MerchantSummary() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
