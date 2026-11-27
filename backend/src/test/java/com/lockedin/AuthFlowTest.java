package com.lockedin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/** Register and log in through the whole stack: HTTP, validation, service, Postgres, JWT. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    void registerThenLogInWithTheSameAccount() throws Exception {
        String email = uniqueEmail();

        MvcResult registered = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email.toUpperCase(), "correct-horse")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.xp").value(0))
            .andReturn();
        String userId = JsonPath.read(registered.getResponse().getContentAsString(), "$.user.id");
        String registerToken = JsonPath.read(registered.getResponse().getContentAsString(), "$.token");
        assertThat(jwtDecoder.decode(registerToken).getSubject()).isEqualTo(userId);

        MvcResult loggedIn = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "correct-horse")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value(userId))
            .andReturn();
        String loginToken = JsonPath.read(loggedIn.getResponse().getContentAsString(), "$.token");
        assertThat(jwtDecoder.decode(loginToken).getSubject()).isEqualTo(userId);
    }

    @Test
    void registeringTheSameEmailTwiceIs409() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "correct-horse")))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email.toUpperCase(), "another-horse")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("An account with this email already exists"));
    }

    @Test
    void aWrongPasswordIs401() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "correct-horse")))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "wrong-horse")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Email or password is incorrect"));
    }

    private static String uniqueEmail() {
        return "flow-" + UUID.randomUUID() + "@example.com";
    }

    private static String body(String email, String password) {
        return "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password);
    }
}
