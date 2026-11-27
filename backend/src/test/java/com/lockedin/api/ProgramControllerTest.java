package com.lockedin.api;

import static com.lockedin.api.SignedIn.USER_ID;
import static com.lockedin.api.SignedIn.signedIn;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lockedin.api.dto.WorkoutExerciseResponse;
import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.NotFoundException;
import com.lockedin.services.ProgramService;

@WebMvcTest(ProgramController.class)
@Import(SecurityConfig.class)
class ProgramControllerTest {

    static final UUID PROGRAM = UUID.fromString("cccccccc-0000-4000-8000-000000000001");
    static final UUID COPY = UUID.fromString("dddddddd-0000-4000-8000-000000000001");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProgramService service;

    @Test
    void listsThePrograms() throws Exception {
        UUID link = UUID.randomUUID();
        UUID exercise = UUID.randomUUID();
        given(service.list()).willReturn(List.of(new WorkoutResponse(PROGRAM, "PPL Push", "Push day",
            Instant.parse("2026-01-01T00:00:00Z"), List.of(new WorkoutExerciseResponse(link, exercise, "Bench")))));

        mvc.perform(get("/api/programs").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(PROGRAM.toString()))
            .andExpect(jsonPath("$[0].name").value("PPL Push"))
            .andExpect(jsonPath("$[0].exercises[0].name").value("Bench"));
    }

    @Test
    void copyReturns201WithTheNewWorkout() throws Exception {
        given(service.copy(USER_ID, PROGRAM)).willReturn(
            new WorkoutResponse(COPY, "PPL Push", "Push day", Instant.parse("2026-06-11T10:00:00Z"), List.of()));

        mvc.perform(post("/api/programs/" + PROGRAM + "/copy").with(signedIn()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(COPY.toString()));
    }

    @Test
    void copyingSomethingThatIsNotAProgramIs404() throws Exception {
        given(service.copy(USER_ID, PROGRAM)).willThrow(new NotFoundException("Program not found"));

        mvc.perform(post("/api/programs/" + PROGRAM + "/copy").with(signedIn()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Program not found"));
    }
}
