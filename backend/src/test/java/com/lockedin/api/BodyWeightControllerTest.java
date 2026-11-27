package com.lockedin.api;

import static com.lockedin.api.SignedIn.USER_ID;
import static com.lockedin.api.SignedIn.signedIn;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lockedin.api.dto.BodyWeightRequest;
import com.lockedin.api.dto.BodyWeightResponse;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.BodyWeightService;
import com.lockedin.services.NotFoundException;

@WebMvcTest(BodyWeightController.class)
@Import(SecurityConfig.class)
class BodyWeightControllerTest {

    static final UUID ID = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000001");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BodyWeightService service;

    @Test
    void listsTheUsersEntries() throws Exception {
        given(service.list(USER_ID)).willReturn(List.of(
            new BodyWeightResponse(ID, 82.5, Instant.parse("2026-06-11T08:00:00Z"))));

        mvc.perform(get("/api/bodyweights").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ID.toString()))
            .andExpect(jsonPath("$[0].weight").value(82.5))
            .andExpect(jsonPath("$[0].createdAt").value("2026-06-11T08:00:00Z"));
    }

    @Test
    void createReturns201() throws Exception {
        given(service.create(USER_ID, new BodyWeightRequest(82.5)))
            .willReturn(new BodyWeightResponse(ID, 82.5, Instant.parse("2026-06-11T08:00:00Z")));

        mvc.perform(post("/api/bodyweights").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{\"weight\": 82.5}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void aWeightOfZeroIs400() throws Exception {
        mvc.perform(post("/api/bodyweights").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{\"weight\": 0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Please enter a weight"));
        verifyNoInteractions(service);
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/bodyweights/" + ID).with(signedIn()))
            .andExpect(status().isNoContent());
        verify(service).delete(USER_ID, ID);
    }

    @Test
    void deletingSomeoneElsesEntryIs404() throws Exception {
        willThrow(new NotFoundException("Body weight entry not found")).given(service).delete(USER_ID, ID);

        mvc.perform(delete("/api/bodyweights/" + ID).with(signedIn()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Body weight entry not found"));
    }
}
