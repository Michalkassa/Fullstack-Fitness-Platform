package com.lockedin.services;

import static com.lockedin.services.StatusAssertions.assertStatus;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.lockedin.JpaModelTest;
import com.lockedin.models.BaseEntity;
import com.lockedin.models.BodyWeight;
import com.lockedin.models.Exercise;
import com.lockedin.models.Log;
import com.lockedin.models.NutritionEntry;
import com.lockedin.models.User;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/**
 * Formats from docs/API.md, "GET /api/export/csv" (lines end with CRLF, as RFC 4180 says) and
 * "GET /api/export/pdf".
 */
@JpaModelTest
@Import({ExportService.class, AnalyticsService.class, ExportServiceTest.FixedClock.class})
class ExportServiceTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-06-20T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    TestEntityManager em;

    @Autowired
    ExportService service;

    User user;
    User other;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("csv@example.com", "hash"));
        other = em.persist(new User("csv-other@example.com", "hash"));
    }

    @Test
    void logsCsvListsEverySetOldestFirst() {
        Exercise bench = em.persist(new Exercise(user, "Barbell Bench Press", ""));
        Exercise squat = em.persist(new Exercise(user, "Squat", ""));
        persist(new Log(user, squat, 102.5, 3, 110), "2026-06-12T09:00:00Z");
        persist(new Log(user, bench, 100, 5, 112), "2026-06-11T23:30:00Z");
        Exercise theirs = em.persist(new Exercise(other, "Theirs", ""));
        persist(new Log(other, theirs, 50, 5, 56), "2026-06-11T10:00:00Z");

        assertThat(service.csv(user.getId(), "logs")).isEqualTo(
            "date,exercise,weight_kg,reps,est_1rm_kg\r\n"
                + "2026-06-11,Barbell Bench Press,100,5,112\r\n"
                + "2026-06-12,Squat,102.5,3,110\r\n");
    }

    @Test
    void fieldsWithCommasQuotesOrLineBreaksAreQuoted() {
        Exercise curl = em.persist(new Exercise(user, "Curl, \"21s\"", ""));
        Exercise twoLines = em.persist(new Exercise(user, "Line\nbreak", ""));
        persist(new Log(user, curl, 20, 21, 34), "2026-06-11T10:00:00Z");
        persist(new Log(user, twoLines, 10, 1, 10), "2026-06-12T10:00:00Z");

        assertThat(service.csv(user.getId(), "logs")).isEqualTo(
            "date,exercise,weight_kg,reps,est_1rm_kg\r\n"
                + "2026-06-11,\"Curl, \"\"21s\"\"\",20,21,34\r\n"
                + "2026-06-12,\"Line\nbreak\",10,1,10\r\n");
    }

    @Test
    void bodyweightCsv() {
        persist(new BodyWeight(user, 81), "2026-06-12T08:00:00Z");
        persist(new BodyWeight(user, 82.5), "2026-06-11T08:00:00Z");
        persist(new BodyWeight(other, 90), "2026-06-11T08:00:00Z");

        assertThat(service.csv(user.getId(), "bodyweight")).isEqualTo(
            "date,weight_kg\r\n"
                + "2026-06-11,82.5\r\n"
                + "2026-06-12,81\r\n");
    }

    @Test
    void nutritionCsv() {
        persist(new NutritionEntry(user, 650, 40, 70, 20), "2026-06-11T12:00:00Z");
        persist(new NutritionEntry(user, 300, 12.5, 0, 0), "2026-06-12T08:00:00Z");
        persist(new NutritionEntry(other, 900), "2026-06-11T08:00:00Z");

        assertThat(service.csv(user.getId(), "nutrition")).isEqualTo(
            "date,calories,protein_g,carbs_g,fat_g\r\n"
                + "2026-06-11,650,40,70,20\r\n"
                + "2026-06-12,300,12.5,0,0\r\n");
    }

    @Test
    void noRowsIsJustTheHeader() {
        assertThat(service.csv(user.getId(), "logs")).isEqualTo("date,exercise,weight_kg,reps,est_1rm_kg\r\n");
    }

    @Test
    void anUnknownTypeIsABadRequest() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.csv(user.getId(), "workouts"));
    }

    @Test
    void pdfIsAOnePageReport() throws IOException {
        PdfReader reader = new PdfReader(service.pdf(user.getId()));

        assertThat(reader.getNumberOfPages()).isEqualTo(1);
    }

    @Test
    void pdfSummarisesTheUsersTraining() throws IOException {
        Exercise bench = em.persist(new Exercise(user, "Barbell Bench Press", ""));
        Exercise squat = em.persist(new Exercise(user, "Squat", ""));
        persist(new Log(user, bench, 100, 5, 112), "2026-06-11T10:00:00Z");
        persist(new Log(user, bench, 90, 8, 111), "2026-06-12T10:00:00Z");
        persist(new Log(user, squat, 102.5, 3, 110), "2026-06-13T10:00:00Z");
        persist(new BodyWeight(user, 82.5), "2026-06-10T08:00:00Z");
        persist(new BodyWeight(user, 81), "2026-06-14T08:00:00Z");
        Exercise theirs = em.persist(new Exercise(other, "Theirs", ""));
        persist(new Log(other, theirs, 500, 10, 666), "2026-06-11T10:00:00Z");
        persist(new BodyWeight(other, 120), "2026-06-15T08:00:00Z");

        String text = pdfText();

        assertThat(text)
            .contains(compact("csv@example.com"))
            .contains(compact("Generated 2026-06-20"))
            .contains(compact("Total sets 3"))
            // 100 × 5 + 90 × 8 + 102.5 × 3
            .contains(compact("Total volume 1527.5 kg"))
            .contains(compact("First body weight 82.5 kg (2026-06-10)"))
            .contains(compact("Latest body weight 81 kg (2026-06-14)"))
            .contains(compact("Barbell Bench Press 100 kg 112 kg 720 kg 2026-06-11"))
            .contains(compact("Squat 102.5 kg 110 kg 307.5 kg 2026-06-13"))
            .doesNotContain(compact("Theirs"))
            .doesNotContain(compact("666"));
    }

    @Test
    void pdfWithNoDataShowsDashesAndNoRecords() throws IOException {
        String text = pdfText();

        assertThat(text)
            .contains(compact("Total sets 0"))
            .contains(compact("Total volume 0 kg"))
            .contains(compact("First body weight \u2014"))
            .contains(compact("Latest body weight \u2014"))
            .contains(compact("No personal records yet"));
    }

    private String pdfText() throws IOException {
        PdfReader reader = new PdfReader(service.pdf(user.getId()));
        return compact(new PdfTextExtractor(reader).getTextFromPage(1));
    }

    /** The extractor drops the space between some table cells, so text is compared without whitespace. */
    private static String compact(String text) {
        return text.replaceAll("\\s+", "");
    }

    private void persist(BaseEntity entity, String createdAt) {
        entity.setCreatedAt(Instant.parse(createdAt));
        em.persistAndFlush(entity);
    }
}
