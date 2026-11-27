package com.lockedin.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.models.BaseEntity;
import com.lockedin.models.BodyWeight;
import com.lockedin.models.User;

@JpaModelTest
class BodyWeightRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    BodyWeightRepository bodyWeights;

    User user;
    User other;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("bw@example.com", "hash"));
        other = em.persist(new User("bw-other@example.com", "hash"));
    }

    @Test
    void listsOnlyTheUsersEntriesOldestFirst() {
        BodyWeight newer = persist(new BodyWeight(user, 81), "2026-06-02T08:00:00Z");
        BodyWeight older = persist(new BodyWeight(user, 82), "2026-06-01T08:00:00Z");
        persist(new BodyWeight(other, 90), "2026-06-01T09:00:00Z");

        assertThat(bodyWeights.findByUserIdOrderByCreatedAtAsc(user.getId()))
            .extracting(BaseEntity::getId)
            .containsExactly(older.getId(), newer.getId());
    }

    @Test
    void findsAnEntryOnlyForItsOwner() {
        BodyWeight entry = persist(new BodyWeight(user, 81), "2026-06-01T08:00:00Z");

        assertThat(bodyWeights.findByIdAndUserId(entry.getId(), user.getId())).isPresent();
        assertThat(bodyWeights.findByIdAndUserId(entry.getId(), other.getId())).isEmpty();
    }

    private BodyWeight persist(BodyWeight entry, String createdAt) {
        entry.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(entry);
    }
}
