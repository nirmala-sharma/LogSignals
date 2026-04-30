package com.nirmala.logsense.service;

import com.nirmala.logsense.dto.*;
import com.nirmala.logsense.entity.Application;
import com.nirmala.logsense.entity.ApplicationApiKey;
import com.nirmala.logsense.entity.User;
import com.nirmala.logsense.exception.AuthenticationException;
import com.nirmala.logsense.repository.AppUserRepository;
import com.nirmala.logsense.repository.ApplicationApiKeyRepository;
import com.nirmala.logsense.repository.ApplicationRepository;
import com.nirmala.logsense.util.ApiKeyUtil;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ApplicationRepository applicationRepository;
    private final ApplicationApiKeyRepository apiKeyRepository;

    public AuthService(AppUserRepository appUserRepository,
                       BCryptPasswordEncoder passwordEncoder, ApplicationRepository applicationRepository,
                       ApplicationApiKeyRepository apiKeyRepository) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.applicationRepository = applicationRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = appUserRepository.save(user);

        Application application = new Application();
        application.setOwnerUserId(savedUser.getUserId());
        application.setName(request.getApplicationName());
        application.setDescription(request.getApplicationDescription());

        Application savedApplication = applicationRepository.save(application);

        String rawApiKey = ApiKeyUtil.generateApiKey();

        ApplicationApiKey apiKey = new ApplicationApiKey();
        apiKey.setApplicationId(savedApplication.getAppId());
        apiKey.setKeyHash(ApiKeyUtil.hashApiKey(rawApiKey));
        apiKey.setKeyPrefix(ApiKeyUtil.getKeyPrefix(rawApiKey));
        apiKey.setName("Default API Key");

        apiKeyRepository.save(apiKey);

        return new RegisterResponseDTO(
                savedUser.getUserId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedApplication.getAppId(),
                savedApplication.getName(),
                rawApiKey,
                "Registration successful. Save this API key now."
        );
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        return new LoginResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                "Login successful"
        );
    }
}
