package com.nirmala.logsense.security;

import com.nirmala.logsense.exception.AuthenticationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private static final byte[] SECRET = "test-secret-that-is-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.parse("2026-04-28T10:00:00Z");

    private JwtTokenService serviceAt(Instant instant) {
        return new JwtTokenService(SECRET, Duration.ofMinutes(60), Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void issuedTokenAuthenticatesAsSameUser() {
        JwtTokenService service = serviceAt(NOW);
        String token = service.issueToken(42L);

        assertEquals(3, token.split("\\.").length);
        assertEquals(42L, service.authenticate("Bearer " + token));
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenService service = serviceAt(NOW);
        String token = service.issueToken(42L);
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"1\",\"iat\":0,\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThrows(AuthenticationException.class, () -> service.verify(forged));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = serviceAt(NOW).issueToken(42L);
        JwtTokenService other = new JwtTokenService(
                "another-secret-that-is-also-32-bytes-long".getBytes(StandardCharsets.UTF_8),
                Duration.ofMinutes(60), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(AuthenticationException.class, () -> other.verify(token));
    }

    @Test
    void rejectsExpiredToken() {
        String token = serviceAt(NOW).issueToken(42L);
        JwtTokenService later = serviceAt(NOW.plus(Duration.ofMinutes(61)));

        AuthenticationException ex = assertThrows(AuthenticationException.class, () -> later.verify(token));
        assertEquals("Access token has expired", ex.getMessage());
    }

    @Test
    void rejectsMissingOrMalformedHeader() {
        JwtTokenService service = serviceAt(NOW);
        assertThrows(AuthenticationException.class, () -> service.authenticate(null));
        assertThrows(AuthenticationException.class, () -> service.authenticate("Basic abc"));
        assertThrows(AuthenticationException.class, () -> service.authenticate("Bearer not-a-token"));
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenService(
                "short".getBytes(StandardCharsets.UTF_8), Duration.ofMinutes(1), Clock.systemUTC()));
    }
}
