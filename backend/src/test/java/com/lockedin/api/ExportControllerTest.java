package com.lockedin.api;

import static com.lockedin.api.SignedIn.USER_ID;
import static com.lockedin.api.SignedIn.signedIn;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.lockedin.config.SecurityConfig;
import com.lockedin.services.ExportService;

@WebMvcTest(ExportController.class)
@Import(SecurityConfig.class)
class ExportControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ExportService service;

    @Test
    void csvDefaultsToLogsAsAnAttachment() throws Exception {
        given(service.csv(USER_ID, "logs")).willReturn("date,exercise,weight_kg,reps,est_1rm_kg\r\n");

        mvc.perform(get("/api/export/csv").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/csv;charset=UTF-8"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lockedin-logs.csv\""))
            .andExpect(content().string("date,exercise,weight_kg,reps,est_1rm_kg\r\n"));
    }

    @Test
    void csvNamesTheFileAfterTheType() throws Exception {
        given(service.csv(USER_ID, "bodyweight")).willReturn("date,weight_kg\r\n2026-06-11,82.5\r\n");

        mvc.perform(get("/api/export/csv").param("type", "bodyweight").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"lockedin-bodyweight.csv\""))
            .andExpect(content().string("date,weight_kg\r\n2026-06-11,82.5\r\n"));
    }

    @Test
    void anUnknownTypeIs400() throws Exception {
        given(service.csv(USER_ID, "workouts")).willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Unknown export type: choose logs, bodyweight or nutrition"));

        mvc.perform(get("/api/export/csv").param("type", "workouts").with(signedIn()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Unknown export type: choose logs, bodyweight or nutrition"));
    }

    @Test
    void pdfIsAnAttachment() throws Exception {
        byte[] pdf = "%PDF-1.4 fake".getBytes();
        given(service.pdf(USER_ID)).willReturn(pdf);

        mvc.perform(get("/api/export/pdf").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"lockedin-progress-report.pdf\""))
            .andExpect(content().bytes(pdf));
    }

    @Test
    void pdfNeedsAToken() throws Exception {
        mvc.perform(get("/api/export/pdf")).andExpect(status().isUnauthorized());
    }
}
