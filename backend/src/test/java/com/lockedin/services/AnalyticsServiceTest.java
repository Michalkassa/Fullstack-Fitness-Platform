package com.lockedin.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.lockedin.JpaModelTest;
import com.lockedin.api.dto.ActivityDay;
import com.lockedin.api.dto.PersonalRecord;
import com.lockedin.api.dto.WeeklyStat;
import com.lockedin.models.Exercise;
import com.lockedin.models.Log;
import com.lockedin.models.User;

/** The examples come from docs/API.md, "Business rules". */
@JpaModelTest
@Import(AnalyticsService.class)
class AnalyticsServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AnalyticsService service;

    User user;
    User other;
    Exercise bench;
    Exercise squat;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("stats-svc@example.com", "hash"));
        other = em.persist(new User("stats-svc-other@example.com", "hash"));
        bench = em.persist(new Exercise(user, "Bench", ""));
        squat = em.persist(new Exercise(user, "Squat", ""));
    }

    @Test
    void weeklyGroupsVolumeAndTrainingDaysByUtcWeekOldestFirst() {
        log(squat, 60, 5, "2026-06-15T10:00:00Z");
        log(bench, 100, 5, "2026-06-08T10:00:00Z");
        log(bench, 50, 10, "2026-06-08T11:00:00Z");
        log(squat, 80, 5, "2026-06-10T10:00:00Z");
        logFor(other, 500, 5, "2026-06-08T10:00:00Z");

        assertThat(service.weekly(user.getId())).containsExactly(
            new WeeklyStat(LocalDate.parse("2026-06-08"), 1400, 2),
            new WeeklyStat(LocalDate.parse("2026-06-15"), 300, 1));
    }

    @Test
    void weeksStartOnMondayUtc() {
        log(bench, 10, 1, "2026-06-14T23:59:59Z");
        log(bench, 10, 1, "2026-06-08T00:00:00Z");

        assertThat(service.weekly(user.getId())).containsExactly(
            new WeeklyStat(LocalDate.parse("2026-06-08"), 20, 2));
    }

    @Test
    void weeklyWithNoLogsIsEmpty() {
        assertThat(service.weekly(user.getId())).isEmpty();
    }

    @Test
    void activityCountsLogsPerUtcDayOldestFirst() {
        log(bench, 100, 5, "2026-06-09T00:30:00Z");
        log(bench, 100, 5, "2026-06-08T10:00:00Z");
        log(squat, 100, 5, "2026-06-08T23:59:59Z");
        logFor(other, 100, 5, "2026-06-08T10:00:00Z");

        assertThat(service.activity(user.getId())).containsExactly(
            new ActivityDay(LocalDate.parse("2026-06-08"), 2),
            new ActivityDay(LocalDate.parse("2026-06-09"), 1));
    }

    @Test
    void recordsPicksTheBestOfEachExerciseSortedByName() {
        log(squat, 140, 3, 148, "2026-06-02T10:00:00Z");
        log(bench, 80, 5, 90, "2026-06-01T10:00:00Z");
        log(bench, 100, 3, 109, "2026-06-08T10:00:00Z");
        Exercise curl = em.persist(new Exercise(user, "Curl", ""));
        logFor(other, 300, 1, "2026-06-01T10:00:00Z");

        assertThat(service.records(user.getId())).containsExactly(
            new PersonalRecord(bench.getId(), "Bench", 100, 109, 400, LocalDate.parse("2026-06-08")),
            new PersonalRecord(squat.getId(), "Squat", 140, 148, 420, LocalDate.parse("2026-06-02")));
        assertThat(service.records(user.getId())).extracting(PersonalRecord::exerciseId).doesNotContain(curl.getId());
    }

    @Test
    void aTiedOneRepMaxCountsFromTheFirstSet() {
        log(bench, 100, 5, 112, "2026-06-10T10:00:00Z");
        log(bench, 100, 5, 112, "2026-06-03T10:00:00Z");

        assertThat(service.records(user.getId())).singleElement()
            .extracting(PersonalRecord::achievedAt).isEqualTo(LocalDate.parse("2026-06-03"));
    }

    private void log(Exercise exercise, double weight, int reps, String createdAt) {
        log(exercise, weight, reps, 0, createdAt);
    }

    private void log(Exercise exercise, double weight, int reps, int oneRepMax, String createdAt) {
        Log log = new Log(user, exercise, weight, reps, oneRepMax);
        log.setCreatedAt(Instant.parse(createdAt));
        em.persistAndFlush(log);
    }

    private void logFor(User owner, double weight, int reps, String createdAt) {
        Exercise theirs = em.persist(new Exercise(owner, "Theirs", ""));
        Log log = new Log(owner, theirs, weight, reps, 0);
        log.setCreatedAt(Instant.parse(createdAt));
        em.persistAndFlush(log);
    }
}
