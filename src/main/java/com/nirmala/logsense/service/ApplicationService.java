package com.nirmala.logsense.service;

import com.nirmala.logsense.dto.CreateApplicationRequestDTO;
import com.nirmala.logsense.dto.CreateApplicationResponseDTO;
import com.nirmala.logsense.entity.Application;
import com.nirmala.logsense.entity.ApplicationApiKey;
import com.nirmala.logsense.exception.ConflictException;
import com.nirmala.logsense.exception.ResourceNotFoundException;
import com.nirmala.logsense.repository.AppUserRepository;
import com.nirmala.logsense.repository.ApplicationApiKeyRepository;
import com.nirmala.logsense.repository.ApplicationRepository;
import com.nirmala.logsense.util.ApiKeyUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final AppUserRepository appUserRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationApiKeyRepository apiKeyRepository;

    public ApplicationService(AppUserRepository appUserRepository,
                              ApplicationRepository applicationRepository,
                              ApplicationApiKeyRepository apiKeyRepository) {
        this.appUserRepository = appUserRepository;
        this.applicationRepository = applicationRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public CreateApplicationResponseDTO createApplication(Long ownerUserId, CreateApplicationRequestDTO request) {
        appUserRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (applicationRepository.existsByOwnerUserIdAndName(ownerUserId, request.getName())) {
            throw new ConflictException("You already have an application named '" + request.getName() + "'");
        }

        Application application = new Application();
        application.setOwnerUserId(ownerUserId);
        application.setName(request.getName());
        application.setDescription(request.getDescription());

        Application savedApplication = applicationRepository.save(application);

        String rawApiKey = ApiKeyUtil.generateApiKey();
        String keyHash = ApiKeyUtil.hashApiKey(rawApiKey);
        String keyPrefix = ApiKeyUtil.getKeyPrefix(rawApiKey);

        ApplicationApiKey apiKey = new ApplicationApiKey();
        apiKey.setApplicationId(savedApplication.getAppId());
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setName(request.getApiKeyName() == null || request.getApiKeyName().isBlank()
                ? "Default API Key"
                : request.getApiKeyName());

        apiKeyRepository.save(apiKey);

        return new CreateApplicationResponseDTO(
                savedApplication.getAppId(),
                savedApplication.getName(),
                rawApiKey,
                keyPrefix
        );
    }
}
