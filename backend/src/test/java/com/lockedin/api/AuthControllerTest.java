package com.lockedin.api;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import com.lockedin.api.dto.LoginRequest;
import com.lockedin.api.dto.RegisterRequest;
import com.lockedin.api.dto.UserResponse;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.TokenService;
import com.lockedin.services.UserService;

/** docs/API.md, "Auth". Both endpoints are public: none of these requests send a token. */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    static final UUID ID = UUID.fromString("3f1c2b9e-4d5a-4e0b-9a51-7d1f0c2e8b11");
    static final UserResponse USER = new UserResponse(ID, "lifter@example.com", null, null, 0);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService users;

    @MockitoBean
    TokenService tokens;

    @Test
    void registerReturns201WithATokenAndTheUser() throws Exception {
        given(users.register(new RegisterRequest("lifter@example.com", "correct-horse"))).willReturn(USER);
        given(tokens.issue(ID)).willReturn("signed.jwt.token");

        post("/api/auth/register", """
                {"email": "lifter@example.com", "password": "correct-horse"}""")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("signed.jwt.token"))
            .andExpect(jsonPath("$.user.id").value(ID.toString()))
            .andExpect(jsonPath("$.user.email").value("lifter@example.com"))
            .andExpect(jsonPath("$.user.name").value(nullValue()))
            .andExpect(jsonPath("$.user.image").value(nullValue()))
            .andExpect(jsonPath("$.user.xp").value(0))
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @ParameterizedTest(name = "{0} / {1} → {2}")
    @CsvSource(nullValues = "null", value = {
        "null, correct-horse, Please enter an email",
        "'', correct-horse, Please enter an email",
        "not-an-email, correct-horse, Not a valid email",
        "lifter@example.com, null, Password must be at least 8 characters",
        "lifter@example.com, short, Password must be at least 8 characters",
    })
    void registerRejectsInvalidInputWithTheDocumentedMessage(String email, String password, String detail)
            throws Exception {
        post("/api/auth/register", json(email, password))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(detail));
        verifyNoInteractions(users, tokens);
    }

    @Test
    void registerWithATakenEmailIs409() throws Exception {
        given(users.register(any())).willThrow(
            new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists"));

        post("/api/auth/register", json("lifter@example.com", "correct-horse"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("An account with this email already exists"));
        verifyNoInteractions(tokens);
    }

    @Test
    void loginReturns200WithATokenAndTheUser() throws Exception {
        given(users.authenticate(new LoginRequest("lifter@example.com", "correct-horse"))).willReturn(USER);
        given(tokens.issue(ID)).willReturn("signed.jwt.token");

        post("/api/auth/login", json("lifter@example.com", "correct-horse"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("signed.jwt.token"))
            .andExpect(jsonPath("$.user.id").value(ID.toString()))
            .andExpect(jsonPath("$.user.email").value("lifter@example.com"));
    }

    @Test
    void loginWithWrongCredentialsIs401() throws Exception {
        given(users.authenticate(any())).willThrow(
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));

        post("/api/auth/login", json("lifter@example.com", "wrong-horse"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Email or password is incorrect"));
        verifyNoInteractions(tokens);
    }

    @ParameterizedTest(name = "{0} / {1} → {2}")
    @CsvSource(nullValues = "null", value = {
        "null, correct-horse, Please enter an email",
        "'  ', correct-horse, Please enter an email",
        "lifter@example.com, null, Please enter a password",
        "lifter@example.com, '', Please enter a password",
    })
    void loginRejectsMissingFieldsWithTheDocumentedMessage(String email, String password, String detail)
            throws Exception {
        post("/api/auth/login", json(email, password))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(detail));
        verifyNoInteractions(users, tokens);
    }

    private ResultActions post(String path, String body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private static String json(String email, String password) {
        return "{\"email\": %s, \"password\": %s}".formatted(quote(email), quote(password));
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
