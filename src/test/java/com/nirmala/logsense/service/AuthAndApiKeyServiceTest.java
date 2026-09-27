package com.nirmala.logsense.service;

import com.nirmala.logsense.dto.LoginRequestDTO;
import com.nirmala.logsense.dto.LoginResponseDTO;
import com.nirmala.logsense.dto.RegisterRequestDTO;
import com.nirmala.logsense.entity.User;
import com.nirmala.logsense.exception.AuthenticationException;
import com.nirmala.logsense.exception.ConflictException;
import com.nirmala.logsense.repository.AppUserRepository;
import com.nirmala.logsense.repository.ApplicationApiKeyRepository;
import com.nirmala.logsense.repository.ApplicationRepository;
import com.nirmala.logsense.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAndApiKeyServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationApiKeyRepository apiKeyRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtTokenService jwt = new JwtTokenService(
            "test-secret-that-is-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8),
            Duration.ofMinutes(60), Clock.systemUTC());

    private AuthService authService;
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(appUserRepository, encoder, applicationRepository, apiKeyRepository, jwt);
        apiKeyService = new ApiKeyService(apiKeyRepository);
    }

    private LoginRequestDTO login(String email, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void loginReturnsValidAccessToken() {
        User user = new User();
        user.setUserId(7L);
        user.setName("Test");
        user.setEmail("test@example.com");
        user.setPasswordHash(encoder.encode("password123"));
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        LoginResponseDTO response = authService.login(login("test@example.com", "password123"));

        assertEquals("Bearer", response.getTokenType());
        assertEquals(7L, jwt.verify(response.getAccessToken()));
    }

    @Test
    void loginWithUnknownEmailIsUnauthorized() {
        when(appUserRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        assertThrows(AuthenticationException.class,
                () -> authService.login(login("nobody@example.com", "password123")));
    }

    @Test
    void registerWithExistingEmailIsConflict() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("taken@example.com");
        when(appUserRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
    }

    @Test
    void missingOrInvalidApiKeyIsUnauthorized() {
        assertThrows(AuthenticationException.class, () -> apiKeyService.getApplicationIdFromApiKey(null));
        assertThrows(AuthenticationException.class, () -> apiKeyService.getApplicationIdFromApiKey(" "));
        assertThrows(AuthenticationException.class, () -> apiKeyService.getApplicationIdFromApiKey("ls_live_wrong"));
    }
}
