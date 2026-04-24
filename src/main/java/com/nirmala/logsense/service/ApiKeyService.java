package com.nirmala.logsense.service;

import com.nirmala.logsense.entity.ApplicationApiKey;
import com.nirmala.logsense.repository.ApplicationApiKeyRepository;
import com.nirmala.logsense.util.ApiKeyUtil;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    private final ApplicationApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApplicationApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public Long getApplicationIdFromApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new RuntimeException("Missing API key");
        }

        String keyHash = ApiKeyUtil.hashApiKey(rawApiKey);

        ApplicationApiKey apiKey = apiKeyRepository
                .findByKeyHashAndRevokedAtIsNull(keyHash)
                .orElseThrow(() -> new RuntimeException("Invalid API key"));

        return apiKey.getApplicationId();
    }
}
