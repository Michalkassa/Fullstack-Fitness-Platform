package com.lockedin.api;

import static com.lockedin.api.SignedIn.signedIn;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lockedin.api.dto.CatalogExerciseResponse;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.ExerciseCatalogService;

@WebMvcTest(ExerciseCatalogController.class)
@Import(SecurityConfig.class)
class ExerciseCatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ExerciseCatalogService service;

    @Test
    void listsTheCatalog() throws Exception {
        UUID id = UUID.randomUUID();
        given(service.list()).willReturn(List.of(
            new CatalogExerciseResponse(id, "Deadlift", "Back", "Barbell", "Hinge at the hips.")));

        mvc.perform(get("/api/exercise-catalog").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id.toString()))
            .andExpect(jsonPath("$[0].name").value("Deadlift"))
            .andExpect(jsonPath("$[0].muscleGroup").value("Back"))
            .andExpect(jsonPath("$[0].equipment").value("Barbell"))
            .andExpect(jsonPath("$[0].instructions").value("Hinge at the hips."));
    }
}
