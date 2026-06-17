package com.paytrail.modules.dev.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateApiKeyRequest {
    @NotBlank
    private String merchantId;
    @NotBlank
    private String description;

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
