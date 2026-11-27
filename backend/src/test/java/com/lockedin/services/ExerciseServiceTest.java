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
import com.lockedin.api.dto.ExerciseRequest;
import com.lockedin.api.dto.ExerciseResponse;
import com.lockedin.api.dto.ExerciseUpdateRequest;
import com.lockedin.api.dto.PopularExerciseResponse;
import com.lockedin.models.Exercise;
import com.lockedin.models.Log;
import com.lockedin.models.User;

@JpaModelTest
@Import(ExerciseService.class)
class ExerciseServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ExerciseService service;

    User user;
    User other;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("ex-svc@example.com", "hash"));
        other = em.persist(new User("ex-svc-other@example.com", "hash"));
    }

    @Test
    void listReturnsTheUsersExercisesNewestFirst() {
        Exercise older = persist(new Exercise(user, "Squat", "Legs"), "2026-06-01T08:00:00Z");
        Exercise newer = persist(new Exercise(user, "Bench", "Chest"), "2026-06-02T08:00:00Z");
        persist(new Exercise(other, "Deadlift", "Back"), "2026-06-03T08:00:00Z");

        assertThat(service.list(user.getId())).extracting(ExerciseResponse::id)
            .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void getReturnsAllFields() {
        Exercise bench = new Exercise(user, "Bench", "Press it");
        bench.setMuscleGroup("Chest");
        bench.setEquipment("Barbell");
        persist(bench, "2026-06-01T08:00:00Z");

        assertThat(service.get(user.getId(), bench.getId())).isEqualTo(new ExerciseResponse(
            bench.getId(), "Bench", "Press it", "Chest", "Barbell", Instant.parse("2026-06-01T08:00:00Z")));
    }

    @Test
    void getForAnotherUsersOrAMissingExerciseIsNotFound() {
        Exercise bench = persist(new Exercise(user, "Bench", ""), "2026-06-01T08:00:00Z");

        assertStatus(HttpStatus.NOT_FOUND, () -> service.get(other.getId(), bench.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.get(user.getId(), UUID.randomUUID()));
    }

    @Test
    void createTrimsTheTextAndKeepsTheLibraryFields() {
        ExerciseResponse created = service.create(user.getId(),
            new ExerciseRequest("  Barbell Bench Press ", " Lower to chest. ", "Chest", "Barbell"));

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Barbell Bench Press");
        assertThat(created.description()).isEqualTo("Lower to chest.");
        assertThat(created.muscleGroup()).isEqualTo("Chest");
        assertThat(created.equipment()).isEqualTo("Barbell");
        assertThat(created.createdAt()).isNotNull();
        assertThat(service.get(user.getId(), created.id())).isEqualTo(created);
    }

    @Test
    void createAllowsMissingLibraryFields() {
        ExerciseResponse created = service.create(user.getId(), new ExerciseRequest("Curl", "Arms", null, null));

        assertThat(created.muscleGroup()).isNull();
        assertThat(created.equipment()).isNull();
    }

    @Test
    void updateChangesOnlyNameAndDescription() {
        Exercise bench = new Exercise(user, "Bench", "Old");
        bench.setMuscleGroup("Chest");
        bench.setEquipment("Barbell");
        persist(bench, "2026-06-01T08:00:00Z");

        ExerciseResponse updated = service.update(user.getId(), bench.getId(),
            new ExerciseUpdateRequest(" Flat Bench ", " New "));

        assertThat(updated).isEqualTo(new ExerciseResponse(
            bench.getId(), "Flat Bench", "New", "Chest", "Barbell", Instant.parse("2026-06-01T08:00:00Z")));
    }

    @Test
    void updatingAnotherUsersExerciseIsNotFound() {
        Exercise bench = persist(new Exercise(user, "Bench", "Old"), "2026-06-01T08:00:00Z");

        assertStatus(HttpStatus.NOT_FOUND,
            () -> service.update(other.getId(), bench.getId(), new ExerciseUpdateRequest("Mine", "Now")));
        assertThat(bench.getName()).isEqualTo("Bench");
    }

    @Test
    void deleteRemovesTheExercise() {
        Exercise bench = persist(new Exercise(user, "Bench", ""), "2026-06-01T08:00:00Z");

        service.delete(user.getId(), bench.getId());
        em.flush();
        em.clear();

        assertThat(em.find(Exercise.class, bench.getId())).isNull();
    }

    @Test
    void deletingAnotherUsersExerciseIsNotFound() {
        Exercise bench = persist(new Exercise(user, "Bench", ""), "2026-06-01T08:00:00Z");

        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(other.getId(), bench.getId()));
        assertThat(em.find(Exercise.class, bench.getId())).isNotNull();
    }

    @Test
    void popularRanksByLogCountUpToTheLimit() {
        Exercise squat = persist(new Exercise(user, "Squat", ""), "2026-06-01T08:00:00Z");
        Exercise bench = persist(new Exercise(user, "Bench", ""), "2026-06-01T09:00:00Z");
        persist(new Exercise(user, "Curl", ""), "2026-06-01T10:00:00Z");
        logs(squat, 1);
        logs(bench, 3);

        assertThat(service.popular(user.getId(), 2)).containsExactly(
            new PopularExerciseResponse(bench.getId(), "Bench", 3),
            new PopularExerciseResponse(squat.getId(), "Squat", 1));
    }

    private Exercise persist(Exercise exercise, String createdAt) {
        exercise.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(exercise);
    }

    private void logs(Exercise exercise, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(new Log(user, exercise, 100, 5, 112));
        }
        em.flush();
    }
}
