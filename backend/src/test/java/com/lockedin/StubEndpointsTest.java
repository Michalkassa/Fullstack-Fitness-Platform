package com.lockedin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.lockedin.config.SecurityConfig;

/**
 * Every endpoint in docs/API.md is mapped at its documented path and answers 501 until it's built.
 * When you implement an endpoint, remove its row here and give it a real controller test.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class StubEndpointsTest {

    private static final String ID = "3f1c2b9e-4d5a-4e0b-9a51-7d1f0c2e8b11";

    @Autowired
    MockMvc mvc;

    static Stream<Arguments> endpoints() {
        String credentials = """
            {"email": "lifter@example.com", "password": "correct-horse"}""";
        String exercise = """
            {"name": "Bench", "description": "Press", "muscleGroup": null, "equipment": null}""";
        String nameAndDescription = """
            {"name": "Push Day", "description": "Chest"}""";
        String set = """
            {"weight": 100, "reps": 5}""";
        String macros = """
            {"calories": 650, "protein": 40, "carbs": 70, "fat": 20}""";
        String session = """
            {"exercises": [{"exerciseId": "%s", "sets": [{"weight": 100, "reps": 5}]}]}""".formatted(ID);
        return Stream.of(
            Arguments.of("POST", "/api/auth/register", credentials),
            Arguments.of("POST", "/api/auth/login", credentials),
            Arguments.of("GET", "/api/users/me", null),
            Arguments.of("GET", "/api/users/me/stats", null),
            Arguments.of("GET", "/api/bodyweights", null),
            Arguments.of("POST", "/api/bodyweights", "{\"weight\": 82.5}"),
            Arguments.of("DELETE", "/api/bodyweights/" + ID, null),
            Arguments.of("GET", "/api/exercises", null),
            Arguments.of("GET", "/api/exercises/popular?limit=5", null),
            Arguments.of("GET", "/api/exercises/" + ID, null),
            Arguments.of("POST", "/api/exercises", exercise),
            Arguments.of("PUT", "/api/exercises/" + ID, nameAndDescription),
            Arguments.of("DELETE", "/api/exercises/" + ID, null),
            Arguments.of("GET", "/api/exercise-catalog", null),
            Arguments.of("GET", "/api/exercises/" + ID + "/logs", null),
            Arguments.of("GET", "/api/exercises/" + ID + "/logs/previous?limit=3", null),
            Arguments.of("POST", "/api/exercises/" + ID + "/logs", set),
            Arguments.of("DELETE", "/api/logs/" + ID, null),
            Arguments.of("GET", "/api/workouts", null),
            Arguments.of("GET", "/api/workouts/" + ID, null),
            Arguments.of("POST", "/api/workouts", nameAndDescription),
            Arguments.of("PUT", "/api/workouts/" + ID, nameAndDescription),
            Arguments.of("DELETE", "/api/workouts/" + ID, null),
            Arguments.of("POST", "/api/workouts/" + ID + "/exercises", "{\"exerciseId\": \"" + ID + "\"}"),
            Arguments.of("DELETE", "/api/workouts/" + ID + "/exercises/" + ID, null),
            Arguments.of("POST", "/api/workouts/" + ID + "/sessions", session),
            Arguments.of("GET", "/api/programs", null),
            Arguments.of("POST", "/api/programs/" + ID + "/copy", null),
            Arguments.of("GET", "/api/nutrition/entries", null),
            Arguments.of("POST", "/api/nutrition/entries", macros),
            Arguments.of("DELETE", "/api/nutrition/entries/" + ID, null),
            Arguments.of("PUT", "/api/nutrition/goal", macros),
            Arguments.of("GET", "/api/nutrition/summary?since=2026-06-11T22:00:00Z", null),
            Arguments.of("GET", "/api/analytics/weekly", null),
            Arguments.of("GET", "/api/analytics/activity", null),
            Arguments.of("GET", "/api/analytics/records", null),
            Arguments.of("GET", "/api/export/csv?type=logs", null),
            Arguments.of("GET", "/api/export/pdf", null));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    void isMappedAndNotImplementedYet(String method, String path, String body) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), path);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        mvc.perform(request)
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.detail").value("This endpoint isn't implemented yet"));
    }

    @Test
    void validationRunsBeforeTheStub() throws Exception {
        mvc.perform(post("/api/exercises/" + ID + "/logs").contentType(MediaType.APPLICATION_JSON).content("""
                {"weight": 100, "reps": 37}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("At most 36 reps per set"));
    }

    @Test
    void anInvalidSetInsideASessionIsReportedWithItsPath() throws Exception {
        mvc.perform(post("/api/workouts/" + ID + "/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"exercises": [{"exerciseId": "%s", "sets": [{"weight": -1, "reps": 5}]}]}""".formatted(ID)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Enter a weight and the reps"))
            .andExpect(jsonPath("$.errors['exercises[0].sets[0].weight']").value("Enter a weight and the reps"));
    }

    @Test
    void anEmptySessionIsRejected() throws Exception {
        mvc.perform(post("/api/workouts/" + ID + "/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"exercises": []}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Log at least one set before finishing"));
    }

    @Test
    void aMalformedIdIsA404() throws Exception {
        mvc.perform(get("/api/workouts/not-a-uuid"))
            .andExpect(status().isNotFound());
    }
}
