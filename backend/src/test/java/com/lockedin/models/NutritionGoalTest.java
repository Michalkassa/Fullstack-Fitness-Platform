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
class NutritionGoalTest {

    @Autowired
    TestEntityManager em;

    User user;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("goal@example.com", "hash"));
    }

    @Test
    void persistsAndReadsBackAllFields() {
        NutritionGoal saved = em.persistFlushFind(new NutritionGoal(user, 2500, 180, 250, 70));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getCalories()).isEqualTo(2500);
        assertThat(saved.getProtein()).isEqualTo(180);
        assertThat(saved.getCarbs()).isEqualTo(250);
        assertThat(saved.getFat()).isEqualTo(70);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void macrosDefaultToZero() {
        NutritionGoal saved = em.persistFlushFind(new NutritionGoal(user, 2000));

        assertThat(saved.getProtein()).isZero();
        assertThat(saved.getCarbs()).isZero();
        assertThat(saved.getFat()).isZero();
    }

    @Test
    void aUserHasAtMostOneGoal() {
        em.persistAndFlush(new NutritionGoal(user, 2000));

        assertThatThrownBy(() -> em.persistAndFlush(new NutritionGoal(user, 2200)))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void updateReplacesTheTargetsAndMovesUpdatedAt() throws InterruptedException {
        NutritionGoal goal = em.persistFlushFind(new NutritionGoal(user, 2000, 150, 200, 60));
        var createdAt = goal.getCreatedAt();
        var before = goal.getUpdatedAt();

        Thread.sleep(5);
        goal.update(2400, 190, 260, 75);
        em.flush();
        em.clear();

        NutritionGoal reloaded = em.find(NutritionGoal.class, goal.getId());
        assertThat(reloaded.getCalories()).isEqualTo(2400);
        assertThat(reloaded.getProtein()).isEqualTo(190);
        assertThat(reloaded.getCarbs()).isEqualTo(260);
        assertThat(reloaded.getFat()).isEqualTo(75);
        assertThat(reloaded.getUpdatedAt()).isAfter(before);
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void requiresUser() {
        assertThatThrownBy(() -> new NutritionGoal(null, 2000)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isDeletedWithItsUser() {
        NutritionGoal goal = em.persistAndFlush(new NutritionGoal(user, 2000));

        em.remove(user);
        em.flush();
        em.clear();

        assertThat(em.find(NutritionGoal.class, goal.getId())).isNull();
    }
}
