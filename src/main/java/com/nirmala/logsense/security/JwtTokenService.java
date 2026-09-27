package com.nirmala.logsense.security;

import com.nirmala.logsense.exception.AuthenticationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Issues and verifies stateless access tokens in standard JWT format (HS256).
 *
 * <p>Token = base64url(header) . base64url(payload) . base64url(HMAC-SHA256 signature).
 * The payload carries the user id ({@code sub}), issued-at ({@code iat}) and expiry ({@code exp}).
 * Implemented with the JDK only, so it needs no extra dependency.</p>
 */
public class JwtTokenService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String ENCODED_HEADER = B64.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
    private static final Pattern SUB = Pattern.compile("\"sub\"\\s*:\\s*\"(\\d+)\"");
    private static final Pattern EXP = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");
    private static final int MIN_SECRET_BYTES = 32;

    private final byte[] secret;
    private final Duration ttl;
    private final Clock clock;

    public JwtTokenService(byte[] secret, Duration ttl, Clock clock) {
        if (secret == null || secret.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.secret = secret.clone();
        this.ttl = ttl;
        this.clock = clock;
    }

    /** Generates a random secret; tokens become invalid when the application restarts. */
    public static byte[] randomSecret() {
        byte[] bytes = new byte[MIN_SECRET_BYTES];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    public Duration getTtl() {
        return ttl;
    }

    public String issueToken(Long userId) {
        long now = clock.instant().getEpochSecond();
        long exp = now + ttl.getSeconds();
        String payloadJson = "{\"sub\":\"" + userId + "\",\"iat\":" + now + ",\"exp\":" + exp + "}";
        String signingInput = ENCODED_HEADER + "." + B64.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + B64.encodeToString(sign(signingInput));
    }

    /**
     * Validates an {@code Authorization} header value ("Bearer &lt;token&gt;") and returns the user id.
     *
     * @throws AuthenticationException if the header is missing, malformed, tampered with or expired
     */
    public Long authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new AuthenticationException("Missing or invalid Authorization header. Expected: Bearer <token>");
        }
        return verify(authorizationHeader.substring(7).trim());
    }

    public Long verify(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3 || !ENCODED_HEADER.equals(parts[0])) {
            throw new AuthenticationException("Invalid access token");
        }

        byte[] expected = sign(parts[0] + "." + parts[1]);
        byte[] actual;
        String payload;
        try {
            actual = B64_DECODER.decode(parts[2]);
            payload = new String(B64_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Invalid access token");
        }

        // constant-time comparison to avoid timing attacks
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new AuthenticationException("Invalid access token");
        }

        Matcher sub = SUB.matcher(payload);
        Matcher exp = EXP.matcher(payload);
        if (!sub.find() || !exp.find()) {
            throw new AuthenticationException("Invalid access token");
        }
        if (clock.instant().getEpochSecond() >= Long.parseLong(exp.group(1))) {
            throw new AuthenticationException("Access token has expired");
        }
        return Long.parseLong(sub.group(1));
    }

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }
}
