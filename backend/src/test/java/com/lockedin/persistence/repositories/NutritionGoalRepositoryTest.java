package com.lockedin.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.models.NutritionGoal;
import com.lockedin.models.User;

@JpaModelTest
class NutritionGoalRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    NutritionGoalRepository goals;

    @Test
    void findsTheUsersGoal() {
        User user = em.persist(new User("goal@example.com", "hash"));
        User other = em.persist(new User("goal-other@example.com", "hash"));
        NutritionGoal goal = em.persistAndFlush(new NutritionGoal(user, 2500, 180, 250, 70));

        assertThat(goals.findByUserId(user.getId()))
            .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(goal.getId()));
        assertThat(goals.findByUserId(other.getId())).isEmpty();
    }
}
