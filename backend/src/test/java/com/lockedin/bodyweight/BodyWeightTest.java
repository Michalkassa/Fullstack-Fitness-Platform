package com.lockedin.bodyweight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.user.User;

@JpaModelTest
class BodyWeightTest {

    @Autowired
    TestEntityManager em;

    private User user() {
        return em.persist(new User("bw@example.com", "hash"));
    }

    @Test
    void persistsAndReadsBackAllFields() {
        User user = user();

        BodyWeight saved = em.persistFlushFind(new BodyWeight(user, 82.5));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getWeight()).isEqualTo(82.5);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void keepsAnExplicitCreatedAt() {
        Instant at = Instant.parse("2026-06-01T08:00:00Z");
        BodyWeight entry = new BodyWeight(user(), 80);
        entry.setCreatedAt(at);

        assertThat(em.persistFlushFind(entry).getCreatedAt()).isEqualTo(at);
    }

    @Test
    void requiresUser() {
        assertThatThrownBy(() -> new BodyWeight(null, 80)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isDeletedWithItsUser() {
        User user = user();
        BodyWeight entry = em.persistAndFlush(new BodyWeight(user, 80));

        em.remove(user);
        em.flush();
        em.clear();

        assertThat(em.find(BodyWeight.class, entry.getId())).isNull();
    }
}
