package com.paytrail.modules.payments.dto;

import com.paytrail.document.PaymentStatus;
import java.time.Instant;

public class PaymentStatusResponse {

    private String reference;
    private String merchantId;
    private Long amount;
    private String currency;
    private PaymentStatus status;
    private String customerEmail;
    private String customerName;
    private String channel;
    private Instant paidAt;
    private String failureReason;
    private Instant refundedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public PaymentStatusResponse() { }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Instant refundedAt) { this.refundedAt = refundedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
