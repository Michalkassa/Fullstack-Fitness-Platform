package com.lockedin.services;

import static com.lockedin.services.StatusAssertions.assertStatus;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.lockedin.JpaModelTest;
import com.lockedin.api.dto.BodyWeightRequest;
import com.lockedin.api.dto.BodyWeightResponse;
import com.lockedin.models.BodyWeight;
import com.lockedin.models.User;

@JpaModelTest
@Import(BodyWeightService.class)
class BodyWeightServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    BodyWeightService service;

    User user;
    User other;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("bw-svc@example.com", "hash"));
        other = em.persist(new User("bw-svc-other@example.com", "hash"));
    }

    @Test
    void listReturnsTheUsersEntriesOldestFirst() {
        BodyWeight newer = persist(new BodyWeight(user, 81), "2026-06-02T08:00:00Z");
        BodyWeight older = persist(new BodyWeight(user, 82.5), "2026-06-01T08:00:00Z");
        persist(new BodyWeight(other, 90), "2026-06-01T09:00:00Z");

        assertThat(service.list(user.getId())).containsExactly(
            new BodyWeightResponse(older.getId(), 82.5, older.getCreatedAt()),
            new BodyWeightResponse(newer.getId(), 81, newer.getCreatedAt()));
    }

    @Test
    void createRecordsTheWeightNow() {
        Instant before = Instant.now().minusSeconds(1);

        BodyWeightResponse created = service.create(user.getId(), new BodyWeightRequest(80.4));

        assertThat(created.id()).isNotNull();
        assertThat(created.weight()).isEqualTo(80.4);
        assertThat(created.createdAt()).isAfter(before);
        assertThat(service.list(user.getId())).containsExactly(created);
    }

    @Test
    void deleteRemovesTheEntry() {
        BodyWeight entry = persist(new BodyWeight(user, 81), "2026-06-01T08:00:00Z");

        service.delete(user.getId(), entry.getId());
        em.flush();
        em.clear();

        assertThat(em.find(BodyWeight.class, entry.getId())).isNull();
    }

    @Test
    void deletingAnotherUsersOrAMissingEntryIsNotFound() {
        BodyWeight entry = persist(new BodyWeight(user, 81), "2026-06-01T08:00:00Z");

        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(other.getId(), entry.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(user.getId(), UUID.randomUUID()));
        assertThat(em.find(BodyWeight.class, entry.getId())).isNotNull();
    }

    private BodyWeight persist(BodyWeight entry, String createdAt) {
        entry.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(entry);
    }
}
