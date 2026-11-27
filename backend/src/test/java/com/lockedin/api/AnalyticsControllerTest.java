package com.lockedin.api;

import static com.lockedin.api.SignedIn.USER_ID;
import static com.lockedin.api.SignedIn.signedIn;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lockedin.api.dto.ActivityDay;
import com.lockedin.api.dto.PersonalRecord;
import com.lockedin.api.dto.WeeklyStat;
import com.lockedin.config.SecurityConfig;
import com.lockedin.services.AnalyticsService;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
class AnalyticsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AnalyticsService service;

    @Test
    void weeklyUsesPlainDates() throws Exception {
        given(service.weekly(USER_ID)).willReturn(List.of(new WeeklyStat(LocalDate.parse("2026-06-08"), 1400, 2)));

        mvc.perform(get("/api/analytics/weekly").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].week").value("2026-06-08"))
            .andExpect(jsonPath("$[0].volume").value(1400))
            .andExpect(jsonPath("$[0].sessions").value(2));
    }

    @Test
    void activity() throws Exception {
        given(service.activity(USER_ID)).willReturn(List.of(new ActivityDay(LocalDate.parse("2026-06-08"), 2)));

        mvc.perform(get("/api/analytics/activity").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].date").value("2026-06-08"))
            .andExpect(jsonPath("$[0].count").value(2));
    }

    @Test
    void records() throws Exception {
        UUID bench = UUID.randomUUID();
        given(service.records(USER_ID)).willReturn(List.of(
            new PersonalRecord(bench, "Bench", 100, 109, 400, LocalDate.parse("2026-06-08"))));

        mvc.perform(get("/api/analytics/records").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].exerciseId").value(bench.toString()))
            .andExpect(jsonPath("$[0].exerciseName").value("Bench"))
            .andExpect(jsonPath("$[0].bestWeight").value(100))
            .andExpect(jsonPath("$[0].bestOneRepMax").value(109))
            .andExpect(jsonPath("$[0].bestVolume").value(400))
            .andExpect(jsonPath("$[0].achievedAt").value("2026-06-08"));
    }
}
