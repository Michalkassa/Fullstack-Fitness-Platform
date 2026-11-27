package com.lockedin.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.lockedin.config.JwtConfig;

class TokenServiceTest {

    static final String SECRET = "test-secret-test-secret-test-secret-123";

    JwtConfig config = new JwtConfig();
    SecretKey key = config.jwtSigningKey(SECRET);
    TokenService tokens = new TokenService(config.jwtEncoder(key), Duration.ofDays(7));
    JwtDecoder decoder = config.jwtDecoder(key);

    @Test
    void issuesAnHs256TokenWhoseSubjectIsTheUserId() {
        UUID userId = UUID.randomUUID();
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        Jwt jwt = decoder.decode(tokens.issue(userId));

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getIssuedAt()).isBetween(before, Instant.now().plusSeconds(1));
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(Duration.ofDays(7)));
    }

    @Test
    void usesTheConfiguredExpiration() {
        TokenService hourly = new TokenService(config.jwtEncoder(key), Duration.ofHours(1));

        Jwt jwt = decoder.decode(hourly.issue(UUID.randomUUID()));

        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(Duration.ofHours(1)));
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        JwtDecoder otherDecoder = config.jwtDecoder(config.jwtSigningKey("another-secret-another-secret-another"));

        String token = tokens.issue(UUID.randomUUID());

        assertThatThrownBy(() -> otherDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsASecretShorterThan32Bytes() {
        assertThatThrownBy(() -> config.jwtSigningKey("too-short"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 bytes");
    }
}
