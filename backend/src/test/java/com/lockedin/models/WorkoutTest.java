package com.lockedin.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;

@JpaModelTest
class WorkoutTest {

    @Autowired
    TestEntityManager em;

    User user;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("wo@example.com", "hash"));
    }

    @Test
    void persistsAndReadsBackAllFields() {
        Workout saved = em.persistFlushFind(new Workout(user, "Push", "Chest day"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getName()).isEqualTo("Push");
        assertThat(saved.getDescription()).isEqualTo("Chest day");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getExercises()).isEmpty();
    }

    @Test
    void isPrivateAndNotATemplateByDefault() {
        Workout saved = em.persistFlushFind(new Workout(user, "Mine", ""));

        assertThat(saved.isTemplate()).isFalse();
        assertThat(saved.isPublic()).isFalse();
    }

    @Test
    void storesTemplateAndPublicFlags() {
        Workout workout = new Workout(user, "Program", "");
        workout.setTemplate(true);
        workout.setPublic(true);

        Workout saved = em.persistFlushFind(workout);

        assertThat(saved.isTemplate()).isTrue();
        assertThat(saved.isPublic()).isTrue();
    }

    @Test
    void addingExercisesPersistsTheLinks() {
        Exercise bench = em.persist(new Exercise(user, "Bench", ""));
        Exercise press = em.persist(new Exercise(user, "Press", ""));
        Workout workout = new Workout(user, "Push", "");
        workout.addExercise(bench);
        workout.addExercise(press);

        Workout saved = em.persistFlushFind(workout);

        List<String> names = saved.getExercises().stream().map(we -> we.getExercise().getName()).toList();
        assertThat(names).containsExactlyInAnyOrder("Bench", "Press");
        assertThat(saved.getExercises()).allSatisfy(we -> {
            assertThat(we.getId()).isNotNull();
            assertThat(we.getWorkout().getId()).isEqualTo(saved.getId());
            assertThat(we.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void addingTheSameExerciseTwiceIsIdempotent() {
        Exercise bench = em.persist(new Exercise(user, "Bench", ""));
        Workout workout = new Workout(user, "Push", "");

        assertThat(workout.addExercise(bench)).isTrue();
        assertThat(workout.addExercise(bench)).isFalse();

        assertThat(em.persistFlushFind(workout).getExercises()).hasSize(1);
    }

    @Test
    void removingAnExerciseDeletesOnlyTheLink() {
        Exercise bench = em.persist(new Exercise(user, "Bench", ""));
        Workout workout = new Workout(user, "Push", "");
        workout.addExercise(bench);
        Workout saved = em.persistFlushFind(workout);

        assertThat(saved.removeExercise(bench)).isTrue();
        em.flush();
        em.clear();

        assertThat(em.find(Workout.class, saved.getId()).getExercises()).isEmpty();
        assertThat(em.find(Exercise.class, bench.getId())).isNotNull();
    }

    @Test
    void removingAnExerciseNotInTheWorkoutReturnsFalse() {
        Exercise bench = em.persist(new Exercise(user, "Bench", ""));

        assertThat(new Workout(user, "Push", "").removeExercise(bench)).isFalse();
    }

    @Test
    void deletingAnExerciseRemovesItFromWorkouts() {
        Exercise bench = em.persist(new Exercise(user, "Bench", ""));
        Workout workout = new Workout(user, "Push", "");
        workout.addExercise(bench);
        Workout saved = em.persistAndFlush(workout);
        em.clear();

        em.remove(em.find(Exercise.class, bench.getId()));
        em.flush();
        em.clear();

        assertThat(em.find(Workout.class, saved.getId()).getExercises()).isEmpty();
    }

    @Test
    void requiresUserNameAndDescription() {
        assertThatThrownBy(() -> new Workout(null, "n", "d")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Workout(user, null, "d")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Workout(user, "n", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isDeletedWithItsUser() {
        Workout workout = em.persistAndFlush(new Workout(user, "Push", ""));

        em.remove(user);
        em.flush();
        em.clear();

        assertThat(em.find(Workout.class, workout.getId())).isNull();
    }

    @Test
    void readsTheSeededProgramTemplates() {
        List<Workout> templates = em.getEntityManager()
            .createQuery("select w from Workout w where w.isTemplate = true and w.isPublic = true", Workout.class)
            .getResultList();

        assertThat(templates).hasSize(7);
        assertThat(templates).allSatisfy(w -> assertThat(w.getExercises()).isNotEmpty());
    }
}
