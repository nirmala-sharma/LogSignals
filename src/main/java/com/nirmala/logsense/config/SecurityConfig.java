package com.nirmala.logsense.config;

import com.nirmala.logsense.security.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public JwtTokenService jwtTokenService(
            @Value("${logsense.auth.jwtSecret:}") String jwtSecret,
            @Value("${logsense.auth.tokenTtlMinutes:60}") long tokenTtlMinutes) {

        byte[] secret;
        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.warn("JWT_SECRET is not set. Using a random secret: tokens will be invalid after a restart.");
            secret = JwtTokenService.randomSecret();
        } else {
            secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return new JwtTokenService(secret, Duration.ofMinutes(tokenTtlMinutes), Clock.systemUTC());
    }
}
