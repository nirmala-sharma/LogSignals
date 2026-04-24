package com.nirmala.logsense.service;

import com.nirmala.logsense.dto.CreateApplicationRequestDTO;
import com.nirmala.logsense.dto.CreateApplicationResponseDTO;
import com.nirmala.logsense.entity.Application;
import com.nirmala.logsense.entity.ApplicationApiKey;
import com.nirmala.logsense.repository.AppUserRepository;
import com.nirmala.logsense.repository.ApplicationApiKeyRepository;
import com.nirmala.logsense.repository.ApplicationRepository;
import com.nirmala.logsense.util.ApiKeyUtil;
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

    public CreateApplicationResponseDTO createApplication(CreateApplicationRequestDTO request) {
        appUserRepository.findById(request.getOwnerUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Application application = new Application();
        application.setOwnerUserId(request.getOwnerUserId());
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
        apiKey.setName(request.getApiKeyName());

        apiKeyRepository.save(apiKey);

        return new CreateApplicationResponseDTO(
                savedApplication.getAppId(),
                savedApplication.getName(),
                rawApiKey,
                keyPrefix
        );
    }
}
