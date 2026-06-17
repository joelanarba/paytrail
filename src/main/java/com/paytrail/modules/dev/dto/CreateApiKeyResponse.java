package com.paytrail.modules.dev.dto;

public class CreateApiKeyResponse {
    private String apiKey;

    public CreateApiKeyResponse(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
