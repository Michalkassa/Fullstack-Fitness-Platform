package com.lockedin.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;

@JpaModelTest
class WorkoutExerciseTest {

    @Autowired
    TestEntityManager em;

    Workout workout;
    Exercise exercise;

    @BeforeEach
    void setUp() {
        User user = em.persist(new User("we@example.com", "hash"));
        workout = em.persist(new Workout(user, "Push", ""));
        exercise = em.persist(new Exercise(user, "Bench", ""));
    }

    @Test
    void persistsAndReadsBackAllFields() {
        WorkoutExercise saved = em.persistFlushFind(new WorkoutExercise(workout, exercise));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getWorkout().getId()).isEqualTo(workout.getId());
        assertThat(saved.getExercise().getId()).isEqualTo(exercise.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void anExerciseAppearsInAWorkoutAtMostOnce() {
        em.persistAndFlush(new WorkoutExercise(workout, exercise));

        assertThatThrownBy(() -> em.persistAndFlush(new WorkoutExercise(workout, exercise)))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void requiresWorkoutAndExercise() {
        assertThatThrownBy(() -> new WorkoutExercise(null, exercise)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WorkoutExercise(workout, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isDeletedWithItsWorkout() {
        WorkoutExercise link = em.persistAndFlush(new WorkoutExercise(workout, exercise));
        em.clear();

        em.remove(em.find(Workout.class, workout.getId()));
        em.flush();
        em.clear();

        assertThat(em.find(WorkoutExercise.class, link.getId())).isNull();
        assertThat(em.find(Exercise.class, exercise.getId())).isNotNull();
    }
}
