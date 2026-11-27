package com.lockedin.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.user.User;

@JpaModelTest
class NutritionEntryTest {

    @Autowired
    TestEntityManager em;

    User user;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("food@example.com", "hash"));
    }

    @Test
    void persistsAndReadsBackAllFields() {
        NutritionEntry saved = em.persistFlushFind(new NutritionEntry(user, 650, 40.5, 70, 18.2));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getCalories()).isEqualTo(650);
        assertThat(saved.getProtein()).isEqualTo(40.5);
        assertThat(saved.getCarbs()).isEqualTo(70);
        assertThat(saved.getFat()).isEqualTo(18.2);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void macrosDefaultToZero() {
        NutritionEntry saved = em.persistFlushFind(new NutritionEntry(user, 300));

        assertThat(saved.getProtein()).isZero();
        assertThat(saved.getCarbs()).isZero();
        assertThat(saved.getFat()).isZero();
    }

    @Test
    void keepsAnExplicitCreatedAt() {
        Instant at = Instant.parse("2026-06-10T12:30:00Z");
        NutritionEntry entry = new NutritionEntry(user, 500);
        entry.setCreatedAt(at);

        assertThat(em.persistFlushFind(entry).getCreatedAt()).isEqualTo(at);
    }

    @Test
    void requiresUser() {
        assertThatThrownBy(() -> new NutritionEntry(null, 100)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isDeletedWithItsUser() {
        NutritionEntry entry = em.persistAndFlush(new NutritionEntry(user, 100));

        em.remove(user);
        em.flush();
        em.clear();

        assertThat(em.find(NutritionEntry.class, entry.getId())).isNull();
    }
}
