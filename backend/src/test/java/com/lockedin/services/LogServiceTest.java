package com.lockedin.services;

import static com.lockedin.services.StatusAssertions.assertStatus;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.lockedin.JpaModelTest;
import com.lockedin.api.dto.LogRequest;
import com.lockedin.api.dto.LogResponse;
import com.lockedin.models.Exercise;
import com.lockedin.models.Log;
import com.lockedin.models.User;

@JpaModelTest
@Import(LogService.class)
class LogServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    LogService service;

    User user;
    User other;
    Exercise squat;
    Exercise bench;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("log-svc@example.com", "hash"));
        other = em.persist(new User("log-svc-other@example.com", "hash"));
        squat = em.persist(new Exercise(user, "Squat", ""));
        bench = em.persist(new Exercise(user, "Bench", ""));
    }

    @Test
    void listReturnsTheExercisesLogsOldestFirst() {
        Log second = persist(new Log(user, squat, 105, 5, 118), "2026-06-02T08:00:00Z");
        Log first = persist(new Log(user, squat, 100, 5, 112), "2026-06-01T08:00:00Z");
        persist(new Log(user, bench, 80, 5, 90), "2026-06-01T09:00:00Z");

        assertThat(service.list(user.getId(), squat.getId())).containsExactly(
            new LogResponse(first.getId(), squat.getId(), 100, 5, 112, first.getCreatedAt()),
            new LogResponse(second.getId(), squat.getId(), 105, 5, 118, second.getCreatedAt()));
    }

    @Test
    void listingLogsOfAnotherUsersExerciseIsNotFound() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.list(other.getId(), squat.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.list(user.getId(), UUID.randomUUID()));
    }

    @Test
    void previousReturnsTheLatestSetsNewestFirst() {
        persist(new Log(user, squat, 100, 5, 112), "2026-06-01T08:00:00Z");
        Log middle = persist(new Log(user, squat, 105, 5, 118), "2026-06-02T08:00:00Z");
        Log newest = persist(new Log(user, squat, 110, 5, 124), "2026-06-03T08:00:00Z");

        assertThat(service.previous(user.getId(), squat.getId(), 2)).extracting(LogResponse::id)
            .containsExactly(newest.getId(), middle.getId());
    }

    @Test
    void previousOfAnotherUsersExerciseIsNotFound() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.previous(other.getId(), squat.getId(), 1));
    }

    @Test
    void createCalculatesTheOneRepMaxAndAwardsXp() {
        LogResponse created = service.create(user.getId(), squat.getId(), new LogRequest(100.0, 5));

        assertThat(created.id()).isNotNull();
        assertThat(created.exerciseId()).isEqualTo(squat.getId());
        assertThat(created.weight()).isEqualTo(100);
        assertThat(created.reps()).isEqualTo(5);
        assertThat(created.oneRepMax()).isEqualTo(112);
        assertThat(created.createdAt()).isNotNull();
        assertThat(em.find(User.class, user.getId()).getXp()).isEqualTo(10);
    }

    @Test
    void createAcceptsABodyweightSet() {
        assertThat(service.create(user.getId(), squat.getId(), new LogRequest(0.0, 12)).oneRepMax()).isZero();
    }

    @Test
    void creatingALogOnAnotherUsersExerciseIsNotFoundAndAwardsNothing() {
        assertStatus(HttpStatus.NOT_FOUND,
            () -> service.create(other.getId(), squat.getId(), new LogRequest(100.0, 5)));
        assertThat(em.find(User.class, other.getId()).getXp()).isZero();
    }

    @Test
    void deleteRemovesTheLogButKeepsTheXp() {
        service.create(user.getId(), squat.getId(), new LogRequest(100.0, 5));
        Log log = persist(new Log(user, squat, 100, 5, 112), "2026-06-01T08:00:00Z");

        service.delete(user.getId(), log.getId());
        em.flush();
        em.clear();

        assertThat(em.find(Log.class, log.getId())).isNull();
        assertThat(em.find(User.class, user.getId()).getXp()).isEqualTo(10);
    }

    @Test
    void deletingAnotherUsersLogIsNotFound() {
        Log log = persist(new Log(user, squat, 100, 5, 112), "2026-06-01T08:00:00Z");

        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(other.getId(), log.getId()));
        assertThat(em.find(Log.class, log.getId())).isNotNull();
    }

    private Log persist(Log log, String createdAt) {
        log.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(log);
    }
}
