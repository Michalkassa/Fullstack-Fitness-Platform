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

import com.lockedin.api.dto.LogRequest;
import com.lockedin.api.dto.WorkoutExerciseResponse;
import com.lockedin.api.dto.WorkoutRequest;
import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.api.dto.WorkoutSessionRequest;
import com.lockedin.api.dto.WorkoutSessionRequest.SessionExercise;
import com.lockedin.api.dto.WorkoutSessionResult;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.NotFoundException;
import com.lockedin.services.WorkoutService;

@WebMvcTest(WorkoutController.class)
@Import(SecurityConfig.class)
class WorkoutControllerTest {

    static final UUID ID = UUID.fromString("dddddddd-0000-4000-8000-000000000001");
    static final UUID LINK = UUID.fromString("dddddddd-0000-4000-8000-000000000002");
    static final UUID EXERCISE = UUID.fromString("eeeeeeee-0000-4000-8000-000000000001");
    static final WorkoutExerciseResponse BENCH_LINK = new WorkoutExerciseResponse(LINK, EXERCISE, "Bench");
    static final WorkoutResponse PUSH = new WorkoutResponse(ID, "Push Day", "Chest",
        Instant.parse("2026-06-01T10:00:00Z"), List.of(BENCH_LINK));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WorkoutService service;

    @Test
    void listsTheUsersWorkoutsWithTheirExercises() throws Exception {
        given(service.list(USER_ID)).willReturn(List.of(PUSH));

        mvc.perform(get("/api/workouts").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ID.toString()))
            .andExpect(jsonPath("$[0].name").value("Push Day"))
            .andExpect(jsonPath("$[0].description").value("Chest"))
            .andExpect(jsonPath("$[0].createdAt").value("2026-06-01T10:00:00Z"))
            .andExpect(jsonPath("$[0].exercises[0].id").value(LINK.toString()))
            .andExpect(jsonPath("$[0].exercises[0].exerciseId").value(EXERCISE.toString()))
            .andExpect(jsonPath("$[0].exercises[0].name").value("Bench"));
    }

    @Test
    void getReturnsOneWorkout() throws Exception {
        given(service.get(USER_ID, ID)).willReturn(PUSH);

        mvc.perform(get("/api/workouts/" + ID).with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Push Day"));
    }

    @Test
    void getForATemplateOrSomeoneElsesWorkoutIs404() throws Exception {
        given(service.get(USER_ID, ID)).willThrow(new NotFoundException("Workout not found"));

        mvc.perform(get("/api/workouts/" + ID).with(signedIn()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Workout not found"));
    }

    @Test
    void aMalformedIdIs404() throws Exception {
        mvc.perform(get("/api/workouts/not-a-uuid").with(signedIn()))
            .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    @Test
    void createReturns201() throws Exception {
        given(service.create(USER_ID, new WorkoutRequest("Push Day", "Chest")))
            .willReturn(new WorkoutResponse(ID, "Push Day", "Chest", Instant.parse("2026-06-01T10:00:00Z"), List.of()));

        mvc.perform(post("/api/workouts").with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Push Day", "description": "Chest"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.exercises").isEmpty());
    }

    @Test
    void createWithoutANameIs400() throws Exception {
        mvc.perform(post("/api/workouts").with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "", "description": "Chest"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Enter a name and a description"));
    }

    @Test
    void updateReturnsTheWorkout() throws Exception {
        given(service.update(USER_ID, ID, new WorkoutRequest("Push A", "New"))).willReturn(PUSH);

        mvc.perform(put("/api/workouts/" + ID).with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Push A", "description": "New"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/workouts/" + ID).with(signedIn()))
            .andExpect(status().isNoContent());
        verify(service).delete(USER_ID, ID);
    }

    @Test
    void addingANewExerciseIs201() throws Exception {
        given(service.addExercise(USER_ID, ID, EXERCISE)).willReturn(new WorkoutService.AddedExercise(BENCH_LINK, true));

        mvc.perform(post("/api/workouts/" + ID + "/exercises").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{\"exerciseId\": \"" + EXERCISE + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(LINK.toString()))
            .andExpect(jsonPath("$.exerciseId").value(EXERCISE.toString()))
            .andExpect(jsonPath("$.name").value("Bench"));
    }

    @Test
    void addingAnExerciseThatIsAlreadyThereIs200() throws Exception {
        given(service.addExercise(USER_ID, ID, EXERCISE)).willReturn(new WorkoutService.AddedExercise(BENCH_LINK, false));

        mvc.perform(post("/api/workouts/" + ID + "/exercises").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{\"exerciseId\": \"" + EXERCISE + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(LINK.toString()));
    }

    @Test
    void addingWithoutAnExerciseIdIs400() throws Exception {
        mvc.perform(post("/api/workouts/" + ID + "/exercises").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Choose an exercise"));
    }

    @Test
    void removeExerciseReturns204() throws Exception {
        mvc.perform(delete("/api/workouts/" + ID + "/exercises/" + LINK).with(signedIn()))
            .andExpect(status().isNoContent());
        verify(service).removeExercise(USER_ID, ID, LINK);
    }

    @Test
    void removingALinkFromAnotherWorkoutIs404() throws Exception {
        willThrow(new NotFoundException("That exercise isn't in this workout"))
            .given(service).removeExercise(USER_ID, ID, LINK);

        mvc.perform(delete("/api/workouts/" + ID + "/exercises/" + LINK).with(signedIn()))
            .andExpect(status().isNotFound());
    }

    @Test
    void finishSessionReturns201WithTheResult() throws Exception {
        WorkoutSessionRequest session = new WorkoutSessionRequest(List.of(
            new SessionExercise(EXERCISE, List.of(new LogRequest(100.0, 5), new LogRequest(100.0, 5)))));
        given(service.finishSession(USER_ID, ID, session)).willReturn(new WorkoutSessionResult(2, 45));

        mvc.perform(post("/api/workouts/" + ID + "/sessions").with(signedIn()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"exercises": [{"exerciseId": "%s", "sets": [{"weight": 100, "reps": 5}, {"weight": 100, "reps": 5}]}]}"""
                    .formatted(EXERCISE)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.logsCreated").value(2))
            .andExpect(jsonPath("$.xpAwarded").value(45));
    }

    @Test
    void anInvalidSetInsideASessionIsReportedWithItsPath() throws Exception {
        mvc.perform(post("/api/workouts/" + ID + "/sessions").with(signedIn()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"exercises": [{"exerciseId": "%s", "sets": [{"weight": -1, "reps": 5}]}]}""".formatted(EXERCISE)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Enter a weight and the reps"))
            .andExpect(jsonPath("$.errors['exercises[0].sets[0].weight']").value("Enter a weight and the reps"));
        verifyNoInteractions(service);
    }

    @Test
    void anEmptySessionIsRejected() throws Exception {
        mvc.perform(post("/api/workouts/" + ID + "/sessions").with(signedIn())
                .contentType(MediaType.APPLICATION_JSON).content("{\"exercises\": []}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Log at least one set before finishing"));
    }
}
