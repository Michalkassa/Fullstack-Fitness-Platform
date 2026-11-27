package com.lockedin.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.models.Exercise;
import com.lockedin.models.User;
import com.lockedin.models.Workout;
import com.lockedin.models.WorkoutExercise;

@JpaModelTest
class WorkoutExerciseRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    WorkoutExerciseRepository links;

    Workout push;
    Workout pull;
    Exercise bench;
    Exercise row;

    @BeforeEach
    void setUp() {
        User user = em.persist(new User("we-repo@example.com", "hash"));
        bench = em.persist(new Exercise(user, "Bench", ""));
        row = em.persist(new Exercise(user, "Row", ""));
        push = new Workout(user, "Push", "");
        push.addExercise(bench);
        push = em.persistAndFlush(push);
        pull = em.persistAndFlush(new Workout(user, "Pull", ""));
    }

    @Test
    void findsTheLinkBetweenAWorkoutAndAnExercise() {
        assertThat(links.findByWorkoutIdAndExerciseId(push.getId(), bench.getId())).isPresent();
        assertThat(links.findByWorkoutIdAndExerciseId(push.getId(), row.getId())).isEmpty();
        assertThat(links.findByWorkoutIdAndExerciseId(pull.getId(), bench.getId())).isEmpty();
    }

    @Test
    void findsALinkOnlyWithinItsWorkout() {
        WorkoutExercise link = push.getExercises().get(0);

        assertThat(links.findByIdAndWorkoutId(link.getId(), push.getId())).isPresent();
        assertThat(links.findByIdAndWorkoutId(link.getId(), pull.getId())).isEmpty();
    }
}
