package com.lockedin.api;

import static com.lockedin.api.SignedIn.USER_ID;
import static com.lockedin.api.SignedIn.signedIn;
import static org.mockito.BDDMockito.given;
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

import com.lockedin.api.dto.Macros;
import com.lockedin.api.dto.MacrosRequest;
import com.lockedin.api.dto.NutritionEntryResponse;
import com.lockedin.api.dto.NutritionSummaryResponse;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.NutritionService;

@WebMvcTest(NutritionController.class)
@Import(SecurityConfig.class)
class NutritionControllerTest {

    static final UUID ID = UUID.fromString("ffffffff-0000-4000-8000-000000000001");
    static final NutritionEntryResponse LUNCH =
        new NutritionEntryResponse(ID, 650, 40, 70, 20, Instant.parse("2026-06-11T12:00:00Z"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    NutritionService service;

    @Test
    void listsTheUsersEntries() throws Exception {
        given(service.listEntries(USER_ID)).willReturn(List.of(LUNCH));

        mvc.perform(get("/api/nutrition/entries").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ID.toString()))
            .andExpect(jsonPath("$[0].calories").value(650))
            .andExpect(jsonPath("$[0].protein").value(40))
            .andExpect(jsonPath("$[0].carbs").value(70))
            .andExpect(jsonPath("$[0].fat").value(20))
            .andExpect(jsonPath("$[0].createdAt").value("2026-06-11T12:00:00Z"));
    }

    @Test
    void createEntryReturns201() throws Exception {
        given(service.createEntry(USER_ID, new MacrosRequest(650, 40.0, 70.0, 20.0))).willReturn(LUNCH);

        mvc.perform(post("/api/nutrition/entries").with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"calories": 650, "protein": 40, "carbs": 70, "fat": 20}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void negativeCaloriesAre400() throws Exception {
        mvc.perform(post("/api/nutrition/entries").with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"calories": -1, "protein": 0, "carbs": 0, "fat": 0}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Calories cannot be negative"));
        verifyNoInteractions(service);
    }

    @Test
    void deleteEntryReturns204() throws Exception {
        mvc.perform(delete("/api/nutrition/entries/" + ID).with(signedIn()))
            .andExpect(status().isNoContent());
        verify(service).deleteEntry(USER_ID, ID);
    }

    @Test
    void setGoalReturnsTheGoal() throws Exception {
        given(service.setGoal(USER_ID, new MacrosRequest(2500, 180.0, 250.0, 80.0)))
            .willReturn(new Macros(2500, 180, 250, 80));

        mvc.perform(put("/api/nutrition/goal").with(signedIn()).contentType(MediaType.APPLICATION_JSON).content("""
                {"calories": 2500, "protein": 180, "carbs": 250, "fat": 80}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.calories").value(2500))
            .andExpect(jsonPath("$.protein").value(180));
    }

    @Test
    void summaryPassesSince() throws Exception {
        Instant since = Instant.parse("2026-06-11T22:00:00Z");
        given(service.summary(USER_ID, since)).willReturn(
            new NutritionSummaryResponse(new Macros(2500, 180, 250, 80), new Macros(1200, 90, 110, 40)));

        mvc.perform(get("/api/nutrition/summary").param("since", "2026-06-11T22:00:00Z").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.goal.calories").value(2500))
            .andExpect(jsonPath("$.consumed.calories").value(1200));
    }

    @Test
    void summaryWithoutSinceOrAGoal() throws Exception {
        given(service.summary(USER_ID, null)).willReturn(new NutritionSummaryResponse(null, new Macros(0, 0, 0, 0)));

        mvc.perform(get("/api/nutrition/summary").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.goal").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.consumed.calories").value(0));
    }
}
