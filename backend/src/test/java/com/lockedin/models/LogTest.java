package com.lockedin.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;

@JpaModelTest
class LogTest {

    @Autowired
    TestEntityManager em;

    User user;
    Exercise exercise;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("log@example.com", "hash"));
        exercise = em.persist(new Exercise(user, "Bench", ""));
    }

    @Test
    void persistsAndReadsBackAllFields() {
        Log saved = em.persistFlushFind(new Log(user, exercise, 100.5, 5, 113));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getExercise().getId()).isEqualTo(exercise.getId());
        assertThat(saved.getWeight()).isEqualTo(100.5);
        assertThat(saved.getReps()).isEqualTo(5);
        assertThat(saved.getOneRepMax()).isEqualTo(113);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void keepsAnExplicitCreatedAt() {
        Instant at = Instant.parse("2026-06-11T18:02:00Z");
        Log log = new Log(user, exercise, 100, 5, 112);
        log.setCreatedAt(at);

        assertThat(em.persistFlushFind(log).getCreatedAt()).isEqualTo(at);
    }

    @Test
    void requiresUserAndExercise() {
        assertThatThrownBy(() -> new Log(null, exercise, 1, 1, 1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Log(user, null, 1, 1, 1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isDeletedWithItsExercise() {
        Log log = em.persistAndFlush(new Log(user, exercise, 100, 5, 112));

        em.remove(exercise);
        em.flush();
        em.clear();

        assertThat(em.find(Log.class, log.getId())).isNull();
    }

    @Test
    void isDeletedWithItsUser() {
        Log log = em.persistAndFlush(new Log(user, exercise, 100, 5, 112));

        em.remove(user);
        em.flush();
        em.clear();

        assertThat(em.find(Log.class, log.getId())).isNull();
    }
}
