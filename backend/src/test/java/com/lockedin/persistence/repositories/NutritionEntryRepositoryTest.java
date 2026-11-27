package com.lockedin.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.models.BaseEntity;
import com.lockedin.models.NutritionEntry;
import com.lockedin.models.User;

@JpaModelTest
class NutritionEntryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    NutritionEntryRepository entries;

    User user;
    User other;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("food@example.com", "hash"));
        other = em.persist(new User("food-other@example.com", "hash"));
    }

    @Test
    void listsOnlyTheUsersEntriesOldestFirst() {
        NutritionEntry later = persist(new NutritionEntry(user, 500), "2026-06-01T12:00:00Z");
        NutritionEntry earlier = persist(new NutritionEntry(user, 300), "2026-06-01T08:00:00Z");
        persist(new NutritionEntry(other, 900), "2026-06-01T09:00:00Z");

        assertThat(entries.findByUserIdOrderByCreatedAtAsc(user.getId()))
            .extracting(BaseEntity::getId)
            .containsExactly(earlier.getId(), later.getId());
    }

    @Test
    void listsTheUsersEntriesFromAnInstantOnwardsIncludingIt() {
        persist(new NutritionEntry(user, 300), "2026-06-11T21:59:59Z");
        NutritionEntry atStart = persist(new NutritionEntry(user, 400), "2026-06-11T22:00:00Z");
        NutritionEntry after = persist(new NutritionEntry(user, 500), "2026-06-12T08:00:00Z");
        persist(new NutritionEntry(other, 900), "2026-06-12T08:00:00Z");

        assertThat(entries.findByUserIdAndCreatedAtGreaterThanEqual(user.getId(), Instant.parse("2026-06-11T22:00:00Z")))
            .extracting(BaseEntity::getId)
            .containsExactlyInAnyOrder(atStart.getId(), after.getId());
    }

    @Test
    void findsAnEntryOnlyForItsOwner() {
        NutritionEntry entry = persist(new NutritionEntry(user, 300), "2026-06-01T08:00:00Z");

        assertThat(entries.findByIdAndUserId(entry.getId(), user.getId())).isPresent();
        assertThat(entries.findByIdAndUserId(entry.getId(), other.getId())).isEmpty();
    }

    private NutritionEntry persist(NutritionEntry entry, String createdAt) {
        entry.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(entry);
    }
}
