package com.lockedin.api;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.lockedin.api.dto.UserResponse;
import com.lockedin.api.dto.UserStatsResponse;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.UserService;

/** docs/API.md, "Users": endpoints 3–4, for the user whose id is the token's subject. */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    static final UUID ID = UUID.fromString("3f1c2b9e-4d5a-4e0b-9a51-7d1f0c2e8b11");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService users;

    @Test
    void meReturnsTheSignedInUser() throws Exception {
        given(users.get(ID)).willReturn(new UserResponse(ID, "lifter@example.com", null, null, 150));

        mvc.perform(get("/api/users/me").with(jwt().jwt(j -> j.subject(ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andExpect(jsonPath("$.email").value("lifter@example.com"))
            .andExpect(jsonPath("$.name").value(nullValue()))
            .andExpect(jsonPath("$.image").value(nullValue()))
            .andExpect(jsonPath("$.xp").value(150))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meForADeletedUserIs401() throws Exception {
        given(users.get(ID)).willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again"));

        mvc.perform(get("/api/users/me").with(jwt().jwt(j -> j.subject(ID.toString()))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Please sign in again"));
    }

    @Test
    void statsReturnsTheLevelProgress() throws Exception {
        given(users.stats(ID)).willReturn(new UserStatsResponse(150, 2, 50, 200, 25));

        mvc.perform(get("/api/users/me/stats").with(jwt().jwt(j -> j.subject(ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.xp").value(150))
            .andExpect(jsonPath("$.level").value(2))
            .andExpect(jsonPath("$.current").value(50))
            .andExpect(jsonPath("$.needed").value(200))
            .andExpect(jsonPath("$.percent").value(25));
    }
}
