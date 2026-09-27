package com.nirmala.logsense.service;

import com.nirmala.logsense.entity.ApplicationApiKey;
import com.nirmala.logsense.exception.AuthenticationException;
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
            throw new AuthenticationException("Missing API key. Send it in the X-API-Key header");
        }

        String keyHash = ApiKeyUtil.hashApiKey(rawApiKey);

        ApplicationApiKey apiKey = apiKeyRepository
                .findByKeyHashAndRevokedAtIsNull(keyHash)
                .orElseThrow(() -> new AuthenticationException("Invalid or revoked API key"));

        return apiKey.getApplicationId();
    }
}
