package com.lockedin.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lockedin.api.dto.UserResponse;
import com.lockedin.config.JwtConfig;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.TokenService;
import com.lockedin.services.UserService;

/**
 * docs/API.md, "Authentication": only register, login and health are public; everything else needs a
 * valid bearer token and answers 401 without one. Tokens here are signed with the real key.
 */
@WebMvcTest({UserController.class, AuthController.class, HealthController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {

    static final UUID ID = UUID.fromString("3f1c2b9e-4d5a-4e0b-9a51-7d1f0c2e8b11");

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtEncoder encoder;

    @MockitoBean
    UserService users;

    @MockitoBean
    TokenService tokens;

    @Test
    void aValidTokenReachesTheEndpointAsThatUser() throws Exception {
        given(users.get(ID)).willReturn(new UserResponse(ID, "lifter@example.com", null, null, 0));

        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(ID, Duration.ofDays(7))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void noTokenIs401WithAProblemDetail() throws Exception {
        mvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Please sign in again"));
    }

    @Test
    void aMalformedTokenIs401() throws Exception {
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Please sign in again"));
    }

    @Test
    void anExpiredTokenIs401() throws Exception {
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(ID, Duration.ofDays(-1))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Please sign in again"));
    }

    @Test
    void aTokenSignedWithAnotherSecretIs401() throws Exception {
        JwtConfig config = new JwtConfig();
        JwtEncoder forged = config.jwtEncoder(config.jwtSigningKey("someone-elses-secret-someone-elses-secret"));

        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(forged, ID, Duration.ofDays(7))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublicEvenWithABadToken() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/health").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")).andExpect(status().isOk());
    }

    @Test
    void loginIgnoresAStaleTokenLeftInTheBrowser() throws Exception {
        given(users.authenticate(any())).willReturn(new UserResponse(ID, "lifter@example.com", null, null, 0));
        given(tokens.issue(ID)).willReturn("fresh.jwt.token");

        mvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(ID, Duration.ofDays(-1)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"lifter@example.com\", \"password\": \"correct-horse\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("fresh.jwt.token"));
    }

    @Test
    void registerIsPublic() throws Exception {
        given(users.register(any())).willReturn(new UserResponse(ID, "lifter@example.com", null, null, 0));
        given(tokens.issue(ID)).willReturn("fresh.jwt.token");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"lifter@example.com\", \"password\": \"correct-horse\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void aCorsPreflightNeedsNoToken() throws Exception {
        mvc.perform(options("/api/users/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    private String token(UUID userId, Duration lifetime) {
        return token(encoder, userId, lifetime);
    }

    private static String token(JwtEncoder encoder, UUID userId, Duration lifetime) {
        // Issued two days ago, so a negative lifetime gives a token that has already expired.
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(userId.toString())
            .issuedAt(now.minus(Duration.ofDays(2)))
            .expiresAt(now.plus(lifetime))
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    }
}
