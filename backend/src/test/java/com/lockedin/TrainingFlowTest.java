package com.lockedin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/**
 * One user's whole journey through the real stack (HTTP, security, validation, services, Postgres):
 * register, create exercises, log a set, finish a workout, then read it all back through stats, analytics and
 * both exports. A second user checks that none of it leaks across accounts.
 *
 * <p>The sets, and what they add up to:
 * <pre>
 *   bench  100 × 5   logged by hand   1RM 112, volume 500
 *   bench   90 × 8   workout session  1RM 111, volume 720
 *   squat 102.5 × 3  workout session  1RM 108, volume 307.5
 *
 *   total volume 1527.5, XP = 10 (one set) + 2 × 10 + 25 (session) = 55
 * </pre>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TrainingFlowTest {

    @Autowired
    MockMvc mvc;

    @Test
    void registerLogTrainAndReadItAllBack() throws Exception {
        String token = register();

        // Exercises and a hand-logged set.
        String bench = id(call(token, post("/api/exercises"),
            "{\"name\": \"Barbell Bench Press\", \"description\": \"Flat bench\"}").andExpect(status().isCreated()));
        String squat = id(call(token, post("/api/exercises"),
            "{\"name\": \"Squat\", \"description\": \"Back squat\"}").andExpect(status().isCreated()));
        call(token, post("/api/exercises/{id}/logs", bench), "{\"weight\": 100, \"reps\": 5}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.oneRepMax").value(112));
        call(token, post("/api/bodyweights"), "{\"weight\": 82.5}").andExpect(status().isCreated());
        call(token, post("/api/bodyweights"), "{\"weight\": 81}").andExpect(status().isCreated());

        // A workout with both exercises, finished as one session.
        String workout = id(call(token, post("/api/workouts"),
            "{\"name\": \"Push Day\", \"description\": \"Heavy\"}").andExpect(status().isCreated()));
        call(token, post("/api/workouts/{id}/exercises", workout), "{\"exerciseId\": \"%s\"}".formatted(bench))
            .andExpect(status().isCreated());
        call(token, post("/api/workouts/{id}/exercises", workout), "{\"exerciseId\": \"%s\"}".formatted(squat))
            .andExpect(status().isCreated());
        call(token, post("/api/workouts/{id}/sessions", workout), """
            {"exercises": [
              {"exerciseId": "%s", "sets": [{"weight": 90, "reps": 8}]},
              {"exerciseId": "%s", "sets": [{"weight": 102.5, "reps": 3}]}
            ]}""".formatted(bench, squat))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.logsCreated").value(2))
            .andExpect(jsonPath("$.xpAwarded").value(45));

        // XP from both the hand-logged set and the session.
        call(token, get("/api/users/me/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.xp").value(55))
            .andExpect(jsonPath("$.level").value(1))
            .andExpect(jsonPath("$.percent").value(55));

        // Everything was logged today (UTC), so it lands on a single day and week.
        String today = JsonPath.read(call(token, get("/api/analytics/activity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].count").value(3))
            .andReturn().getResponse().getContentAsString(), "$[0].date");
        String monday = LocalDate.parse(today).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();

        call(token, get("/api/analytics/weekly"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].week").value(monday))
            .andExpect(jsonPath("$[0].volume").value(1527.5))
            .andExpect(jsonPath("$[0].sessions").value(1));

        call(token, get("/api/analytics/records"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].exerciseName").value("Barbell Bench Press"))
            .andExpect(jsonPath("$[0].bestWeight").value(100.0))
            .andExpect(jsonPath("$[0].bestOneRepMax").value(112))
            .andExpect(jsonPath("$[0].bestVolume").value(720.0))
            .andExpect(jsonPath("$[0].achievedAt").value(today))
            .andExpect(jsonPath("$[1].exerciseName").value("Squat"))
            .andExpect(jsonPath("$[1].bestOneRepMax").value(108));

        // Exports read the same rows.
        String csv = call(token, get("/api/export/csv").param("type", "logs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(csv.split("\r\n")).containsExactly(
            "date,exercise,weight_kg,reps,est_1rm_kg",
            today + ",Barbell Bench Press,100,5,112",
            today + ",Barbell Bench Press,90,8,111",
            today + ",Squat,102.5,3,108");

        byte[] pdf = call(token, get("/api/export/pdf"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn().getResponse().getContentAsByteArray();
        String report = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1).replaceAll("\\s+", "");
        assertThat(report)
            .contains("Totalsets3")
            .contains("Totalvolume1527.5kg")
            .contains("Firstbodyweight82.5kg")
            .contains("Latestbodyweight81kg")
            .contains("BarbellBenchPress100kg112kg720kg");

        // A second account sees none of it.
        String stranger = register();
        call(stranger, get("/api/exercises/{id}/logs", bench)).andExpect(status().isNotFound());
        call(stranger, post("/api/exercises/{id}/logs", bench), "{\"weight\": 1, \"reps\": 1}")
            .andExpect(status().isNotFound());
        call(stranger, get("/api/analytics/records")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        call(stranger, get("/api/users/me/stats")).andExpect(jsonPath("$.xp").value(0));
    }

    @Test
    void anInvalidSetRollsBackTheWholeSession() throws Exception {
        String token = register();
        String bench = id(call(token, post("/api/exercises"),
            "{\"name\": \"Bench\", \"description\": \"Flat bench\"}").andExpect(status().isCreated()));
        String workout = id(call(token, post("/api/workouts"),
            "{\"name\": \"Push Day\", \"description\": \"Heavy\"}").andExpect(status().isCreated()));
        call(token, post("/api/workouts/{id}/exercises", workout), "{\"exerciseId\": \"%s\"}".formatted(bench))
            .andExpect(status().isCreated());

        // The second exercise isn't the user's, so the session fails after the first set is valid.
        call(token, post("/api/workouts/{id}/sessions", workout), """
            {"exercises": [
              {"exerciseId": "%s", "sets": [{"weight": 100, "reps": 5}]},
              {"exerciseId": "%s", "sets": [{"weight": 100, "reps": 5}]}
            ]}""".formatted(bench, UUID.randomUUID()))
            .andExpect(status().isNotFound());

        call(token, get("/api/exercises/{id}/logs", bench)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        call(token, get("/api/users/me/stats")).andExpect(jsonPath("$.xp").value(0));
    }

    /** Registers a fresh account and returns its token. */
    private String register() throws Exception {
        String body = "{\"email\": \"train-%s@example.com\", \"password\": \"correct-horse\"}".formatted(UUID.randomUUID());
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private ResultActions call(String token, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions call(String token, MockHttpServletRequestBuilder request, String json) throws Exception {
        return call(token, request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String id(ResultActions created) throws Exception {
        return JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
