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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.lockedin.api.dto.ExerciseRequest;
import com.lockedin.api.dto.ExerciseResponse;
import com.lockedin.api.dto.ExerciseUpdateRequest;
import com.lockedin.api.dto.PopularExerciseResponse;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.ExerciseService;
import com.lockedin.services.NotFoundException;

@WebMvcTest(ExerciseController.class)
@Import(SecurityConfig.class)
class ExerciseControllerTest {

    static final UUID ID = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    static final Instant AT = Instant.parse("2026-06-01T10:00:00Z");
    static final ExerciseResponse BENCH = new ExerciseResponse(ID, "Bench", "Press", "Chest", "Barbell", AT);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ExerciseService service;

    @Test
    void listsTheUsersExercises() throws Exception {
        given(service.list(USER_ID)).willReturn(List.of(BENCH));

        mvc.perform(get("/api/exercises").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ID.toString()))
            .andExpect(jsonPath("$[0].name").value("Bench"))
            .andExpect(jsonPath("$[0].description").value("Press"))
            .andExpect(jsonPath("$[0].muscleGroup").value("Chest"))
            .andExpect(jsonPath("$[0].equipment").value("Barbell"))
            .andExpect(jsonPath("$[0].createdAt").value("2026-06-01T10:00:00Z"));
    }

    @Test
    void popularDefaultsToFive() throws Exception {
        given(service.popular(USER_ID, 5)).willReturn(List.of(new PopularExerciseResponse(ID, "Bench", 42)));

        mvc.perform(get("/api/exercises/popular").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ID.toString()))
            .andExpect(jsonPath("$[0].name").value("Bench"))
            .andExpect(jsonPath("$[0].logCount").value(42));
    }

    @Test
    void popularTakesALimit() throws Exception {
        given(service.popular(USER_ID, 3)).willReturn(List.of());

        mvc.perform(get("/api/exercises/popular").param("limit", "3").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void getReturnsOneExercise() throws Exception {
        given(service.get(USER_ID, ID)).willReturn(BENCH);

        mvc.perform(get("/api/exercises/" + ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Bench"));
    }

    @Test
    void getForSomeoneElsesExerciseIs404() throws Exception {
        given(service.get(USER_ID, ID)).willThrow(new NotFoundException("Exercise not found"));

        mvc.perform(get("/api/exercises/" + ID).with(signedIn()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Exercise not found"));
    }

    @Test
    void createReturns201() throws Exception {
        given(service.create(USER_ID, new ExerciseRequest("Bench", "Press", "Chest", "Barbell"))).willReturn(BENCH);

        mvc.perform(post("/api/exercises").with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Bench", "description": "Press", "muscleGroup": "Chest", "equipment": "Barbell"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void createWithoutADescriptionIs400() throws Exception {
        mvc.perform(post("/api/exercises").with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Bench", "description": " ", "muscleGroup": null, "equipment": null}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Enter a name and a description"));
        verifyNoInteractions(service);
    }

    @Test
    void aNameOver40CharactersIs400() throws Exception {
        mvc.perform(post("/api/exercises").with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "%s", "description": "Press", "muscleGroup": null, "equipment": null}""".formatted("x".repeat(41))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Name must be at most 40 characters"));
    }

    @Test
    void updateReturnsTheExercise() throws Exception {
        given(service.update(USER_ID, ID, new ExerciseUpdateRequest("Flat Bench", "New"))).willReturn(
            new ExerciseResponse(ID, "Flat Bench", "New", "Chest", "Barbell", AT));

        mvc.perform(put("/api/exercises/" + ID).with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Flat Bench", "description": "New"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Flat Bench"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/exercises/" + ID).with(signedIn()))
            .andExpect(status().isNoContent());
        verify(service).delete(USER_ID, ID);
    }
}
