package com.lockedin.services;

import static com.lockedin.services.StatusAssertions.assertStatus;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.lockedin.JpaModelTest;
import com.lockedin.api.dto.LogRequest;
import com.lockedin.api.dto.WorkoutExerciseResponse;
import com.lockedin.api.dto.WorkoutRequest;
import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.api.dto.WorkoutSessionRequest;
import com.lockedin.api.dto.WorkoutSessionRequest.SessionExercise;
import com.lockedin.api.dto.WorkoutSessionResult;
import com.lockedin.models.Exercise;
import com.lockedin.models.Log;
import com.lockedin.models.User;
import com.lockedin.models.Workout;
import com.lockedin.models.WorkoutExercise;

@JpaModelTest
@Import(WorkoutService.class)
class WorkoutServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    WorkoutService service;

    User user;
    User other;
    Exercise bench;
    Exercise press;
    Exercise dips;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("wo-svc@example.com", "hash"));
        other = em.persist(new User("wo-svc-other@example.com", "hash"));
        bench = em.persist(new Exercise(user, "Bench", ""));
        press = em.persist(new Exercise(user, "Press", ""));
        dips = em.persist(new Exercise(user, "Dips", ""));
        em.flush();
    }

    @Test
    void listReturnsTheUsersWorkoutsNewestFirstWithTheirExercises() {
        Workout older = persist(new Workout(user, "Pull", "Back"), "2026-06-01T08:00:00Z");
        Workout newer = new Workout(user, "Push", "Chest");
        newer.addExercise(bench);
        persist(newer, "2026-06-02T08:00:00Z");
        persist(template(user, "Template"), "2026-06-03T08:00:00Z");
        persist(new Workout(other, "Legs", ""), "2026-06-03T08:00:00Z");

        List<WorkoutResponse> workouts = service.list(user.getId());

        assertThat(workouts).extracting(WorkoutResponse::id).containsExactly(newer.getId(), older.getId());
        WorkoutExercise link = newer.getExercises().get(0);
        assertThat(workouts.get(0)).isEqualTo(new WorkoutResponse(newer.getId(), "Push", "Chest",
            Instant.parse("2026-06-02T08:00:00Z"),
            List.of(new WorkoutExerciseResponse(link.getId(), bench.getId(), "Bench"))));
        assertThat(workouts.get(1).exercises()).isEmpty();
    }

    @Test
    void getForAnotherUsersWorkoutATemplateOrAMissingOneIsNotFound() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");
        Workout tmpl = persist(template(user, "Template"), "2026-06-01T08:00:00Z");

        assertThat(service.get(user.getId(), push.getId()).name()).isEqualTo("Push");
        assertStatus(HttpStatus.NOT_FOUND, () -> service.get(other.getId(), push.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.get(user.getId(), tmpl.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.get(user.getId(), UUID.randomUUID()));
    }

    @Test
    void createStartsEmptyWithTrimmedText() {
        WorkoutResponse created = service.create(user.getId(), new WorkoutRequest(" Push Day ", " Chest "));

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Push Day");
        assertThat(created.description()).isEqualTo("Chest");
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.exercises()).isEmpty();
        assertThat(service.get(user.getId(), created.id())).isEqualTo(created);
    }

    @Test
    void updateChangesNameAndDescriptionAndKeepsTheExercises() {
        Workout push = new Workout(user, "Push", "Old");
        push.addExercise(bench);
        persist(push, "2026-06-01T08:00:00Z");

        WorkoutResponse updated = service.update(user.getId(), push.getId(), new WorkoutRequest(" Push A ", "New"));

        assertThat(updated.name()).isEqualTo("Push A");
        assertThat(updated.description()).isEqualTo("New");
        assertThat(updated.exercises()).extracting(WorkoutExerciseResponse::exerciseId).containsExactly(bench.getId());
    }

    @Test
    void updatingAnotherUsersWorkoutOrATemplateIsNotFound() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");
        Workout tmpl = persist(template(user, "Template"), "2026-06-01T08:00:00Z");

        assertStatus(HttpStatus.NOT_FOUND,
            () -> service.update(other.getId(), push.getId(), new WorkoutRequest("Mine", "Now")));
        assertStatus(HttpStatus.NOT_FOUND,
            () -> service.update(user.getId(), tmpl.getId(), new WorkoutRequest("Mine", "Now")));
    }

    @Test
    void deleteRemovesTheWorkoutButKeepsTheExercisesAndTheirLogs() {
        Workout push = new Workout(user, "Push", "");
        push.addExercise(bench);
        persist(push, "2026-06-01T08:00:00Z");
        Log log = em.persistAndFlush(new Log(user, bench, 100, 5, 112));

        service.delete(user.getId(), push.getId());
        em.flush();
        em.clear();

        assertThat(em.find(Workout.class, push.getId())).isNull();
        assertThat(em.find(Exercise.class, bench.getId())).isNotNull();
        assertThat(em.find(Log.class, log.getId())).isNotNull();
    }

    @Test
    void deletingAnotherUsersWorkoutOrATemplateIsNotFound() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");
        Workout tmpl = persist(template(user, "Template"), "2026-06-01T08:00:00Z");

        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(other.getId(), push.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(user.getId(), tmpl.getId()));
    }

    @Test
    void addExerciseCreatesALinkTheFirstTimeAndReturnsTheSameOneAfterwards() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");

        WorkoutService.AddedExercise first = service.addExercise(user.getId(), push.getId(), bench.getId());
        WorkoutService.AddedExercise again = service.addExercise(user.getId(), push.getId(), bench.getId());

        assertThat(first.created()).isTrue();
        assertThat(first.exercise().id()).isNotNull();
        assertThat(first.exercise().exerciseId()).isEqualTo(bench.getId());
        assertThat(first.exercise().name()).isEqualTo("Bench");
        assertThat(again.created()).isFalse();
        assertThat(again.exercise()).isEqualTo(first.exercise());
        assertThat(service.get(user.getId(), push.getId()).exercises()).hasSize(1);
    }

    @Test
    void exercisesStayInTheOrderTheyWereAdded() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");

        service.addExercise(user.getId(), push.getId(), press.getId());
        service.addExercise(user.getId(), push.getId(), dips.getId());
        service.addExercise(user.getId(), push.getId(), bench.getId());
        em.flush();
        em.clear();

        assertThat(service.get(user.getId(), push.getId()).exercises())
            .extracting(WorkoutExerciseResponse::exerciseId)
            .containsExactly(press.getId(), dips.getId(), bench.getId());
    }

    @Test
    void addingToAnotherUsersWorkoutOrAddingAnotherUsersExerciseIsNotFound() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");
        Exercise theirs = em.persistAndFlush(new Exercise(other, "Theirs", ""));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.addExercise(other.getId(), push.getId(), theirs.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.addExercise(user.getId(), push.getId(), theirs.getId()));
        assertThat(service.get(user.getId(), push.getId()).exercises()).isEmpty();
    }

    @Test
    void removeExerciseDeletesTheLinkButNotTheExercise() {
        Workout push = new Workout(user, "Push", "");
        push.addExercise(bench);
        push.addExercise(press);
        persist(push, "2026-06-01T08:00:00Z");
        UUID linkId = push.getExercises().get(0).getId();

        service.removeExercise(user.getId(), push.getId(), linkId);
        em.flush();
        em.clear();

        assertThat(service.get(user.getId(), push.getId()).exercises())
            .extracting(WorkoutExerciseResponse::exerciseId).containsExactly(press.getId());
        assertThat(em.find(Exercise.class, bench.getId())).isNotNull();
    }

    @Test
    void removingALinkFromAnotherWorkoutOrAnotherUsersWorkoutIsNotFound() {
        Workout push = new Workout(user, "Push", "");
        push.addExercise(bench);
        persist(push, "2026-06-01T08:00:00Z");
        Workout pull = persist(new Workout(user, "Pull", ""), "2026-06-01T08:00:00Z");
        UUID linkId = push.getExercises().get(0).getId();

        assertStatus(HttpStatus.NOT_FOUND, () -> service.removeExercise(user.getId(), pull.getId(), linkId));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.removeExercise(other.getId(), push.getId(), linkId));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.removeExercise(user.getId(), push.getId(), UUID.randomUUID()));
    }

    @Test
    void finishSessionSavesEverySetWithOneTimestampAndAwardsXp() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");

        WorkoutSessionResult result = service.finishSession(user.getId(), push.getId(), new WorkoutSessionRequest(List.of(
            new SessionExercise(bench.getId(), List.of(new LogRequest(100.0, 5), new LogRequest(100.0, 5))),
            new SessionExercise(press.getId(), List.of(new LogRequest(60.0, 8))))));
        em.flush();

        assertThat(result).isEqualTo(new WorkoutSessionResult(3, 55));
        assertThat(em.find(User.class, user.getId()).getXp()).isEqualTo(55);
        List<Log> logs = em.getEntityManager()
            .createQuery("select l from Log l where l.user.id = :userId", Log.class)
            .setParameter("userId", user.getId())
            .getResultList();
        assertThat(logs).hasSize(3);
        assertThat(logs).extracting(Log::getCreatedAt).containsOnly(logs.get(0).getCreatedAt());
        assertThat(logs).extracting(Log::getOneRepMax).containsExactlyInAnyOrder(112, 112, 74);
    }

    @Test
    void finishSessionWithAnotherUsersExerciseSavesNothing() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");
        Exercise theirs = em.persistAndFlush(new Exercise(other, "Theirs", ""));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.finishSession(user.getId(), push.getId(),
            new WorkoutSessionRequest(List.of(
                new SessionExercise(bench.getId(), List.of(new LogRequest(100.0, 5))),
                new SessionExercise(theirs.getId(), List.of(new LogRequest(60.0, 8)))))));
        em.flush();

        assertThat(logCount()).isZero();
        assertThat(em.find(User.class, user.getId()).getXp()).isZero();
    }

    @Test
    void finishSessionOnAnotherUsersWorkoutOrATemplateIsNotFound() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");
        Workout tmpl = persist(template(user, "Template"), "2026-06-01T08:00:00Z");
        WorkoutSessionRequest session = new WorkoutSessionRequest(List.of(
            new SessionExercise(bench.getId(), List.of(new LogRequest(100.0, 5)))));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.finishSession(other.getId(), push.getId(), session));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.finishSession(user.getId(), tmpl.getId(), session));
        assertThat(logCount()).isZero();
    }

    private long logCount() {
        return em.getEntityManager().createQuery("select count(l) from Log l", Long.class).getSingleResult();
    }

    private static Workout template(User owner, String name) {
        Workout workout = new Workout(owner, name, "");
        workout.setTemplate(true);
        workout.setPublic(true);
        return workout;
    }

    private Workout persist(Workout workout, String createdAt) {
        workout.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(workout);
    }
}
