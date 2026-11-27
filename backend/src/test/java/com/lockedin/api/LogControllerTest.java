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

import com.lockedin.api.dto.LogRequest;
import com.lockedin.api.dto.LogResponse;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.LogService;
import com.lockedin.services.NotFoundException;

@WebMvcTest(LogController.class)
@Import(SecurityConfig.class)
class LogControllerTest {

    static final UUID EXERCISE = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    static final UUID LOG = UUID.fromString("11111111-0000-4000-8000-000000000001");
    static final LogResponse SET = new LogResponse(LOG, EXERCISE, 100, 5, 112, Instant.parse("2026-06-11T18:02:00Z"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LogService service;

    @Test
    void listsTheExercisesLogs() throws Exception {
        given(service.list(USER_ID, EXERCISE)).willReturn(List.of(SET));

        mvc.perform(get("/api/exercises/" + EXERCISE + "/logs").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(LOG.toString()))
            .andExpect(jsonPath("$[0].exerciseId").value(EXERCISE.toString()))
            .andExpect(jsonPath("$[0].weight").value(100))
            .andExpect(jsonPath("$[0].reps").value(5))
            .andExpect(jsonPath("$[0].oneRepMax").value(112))
            .andExpect(jsonPath("$[0].createdAt").value("2026-06-11T18:02:00Z"));
    }

    @Test
    void listingLogsOfSomeoneElsesExerciseIs404() throws Exception {
        given(service.list(USER_ID, EXERCISE)).willThrow(new NotFoundException("Exercise not found"));

        mvc.perform(get("/api/exercises/" + EXERCISE + "/logs").with(signedIn()))
            .andExpect(status().isNotFound());
    }

    @Test
    void previousPassesTheLimit() throws Exception {
        given(service.previous(USER_ID, EXERCISE, 3)).willReturn(List.of(SET));

        mvc.perform(get("/api/exercises/" + EXERCISE + "/logs/previous").param("limit", "3").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(LOG.toString()));
    }

    @Test
    void createReturns201() throws Exception {
        given(service.create(USER_ID, EXERCISE, new LogRequest(100.0, 5))).willReturn(SET);

        mvc.perform(post("/api/exercises/" + EXERCISE + "/logs").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{\"weight\": 100, \"reps\": 5}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.oneRepMax").value(112));
    }

    @Test
    void moreThan36RepsIs400() throws Exception {
        mvc.perform(post("/api/exercises/" + EXERCISE + "/logs").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{\"weight\": 100, \"reps\": 37}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("At most 36 reps per set"));
        verifyNoInteractions(service);
    }

    @Test
    void aMissingWeightIs400() throws Exception {
        mvc.perform(post("/api/exercises/" + EXERCISE + "/logs").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reps\": 5}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Enter a weight and the reps"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/logs/" + LOG).with(signedIn()))
            .andExpect(status().isNoContent());
        verify(service).delete(USER_ID, LOG);
    }
}
