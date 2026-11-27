package com.lockedin.services;

import static com.lockedin.services.StatusAssertions.assertStatus;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.lockedin.JpaModelTest;
import com.lockedin.api.dto.Macros;
import com.lockedin.api.dto.MacrosRequest;
import com.lockedin.api.dto.NutritionEntryResponse;
import com.lockedin.api.dto.NutritionSummaryResponse;
import com.lockedin.models.NutritionEntry;
import com.lockedin.models.NutritionGoal;
import com.lockedin.models.User;

@JpaModelTest
@Import(NutritionService.class)
class NutritionServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    NutritionService service;

    User user;
    User other;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("food-svc@example.com", "hash"));
        other = em.persist(new User("food-svc-other@example.com", "hash"));
    }

    @Test
    void listEntriesReturnsTheUsersEntriesOldestFirst() {
        NutritionEntry later = persist(new NutritionEntry(user, 650, 40, 70, 20), "2026-06-11T12:00:00Z");
        NutritionEntry earlier = persist(new NutritionEntry(user, 300), "2026-06-11T08:00:00Z");
        persist(new NutritionEntry(other, 900), "2026-06-11T09:00:00Z");

        assertThat(service.listEntries(user.getId())).containsExactly(
            new NutritionEntryResponse(earlier.getId(), 300, 0, 0, 0, earlier.getCreatedAt()),
            new NutritionEntryResponse(later.getId(), 650, 40, 70, 20, later.getCreatedAt()));
    }

    @Test
    void createEntryTreatsMissingMacrosAsZero() {
        NutritionEntryResponse created = service.createEntry(user.getId(), new MacrosRequest(400, 30.0, null, null));

        assertThat(created.id()).isNotNull();
        assertThat(created.calories()).isEqualTo(400);
        assertThat(created.protein()).isEqualTo(30);
        assertThat(created.carbs()).isZero();
        assertThat(created.fat()).isZero();
        assertThat(created.createdAt()).isNotNull();
        assertThat(service.listEntries(user.getId())).containsExactly(created);
    }

    @Test
    void deleteEntryRemovesIt() {
        NutritionEntry entry = persist(new NutritionEntry(user, 300), "2026-06-11T08:00:00Z");

        service.deleteEntry(user.getId(), entry.getId());
        em.flush();
        em.clear();

        assertThat(em.find(NutritionEntry.class, entry.getId())).isNull();
    }

    @Test
    void deletingAnotherUsersOrAMissingEntryIsNotFound() {
        NutritionEntry entry = persist(new NutritionEntry(user, 300), "2026-06-11T08:00:00Z");

        assertStatus(HttpStatus.NOT_FOUND, () -> service.deleteEntry(other.getId(), entry.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.deleteEntry(user.getId(), UUID.randomUUID()));
        assertThat(em.find(NutritionEntry.class, entry.getId())).isNotNull();
    }

    @Test
    void setGoalCreatesTheGoalThenReplacesIt() {
        Macros first = service.setGoal(user.getId(), new MacrosRequest(2500, 180.0, 250.0, 80.0));
        Macros second = service.setGoal(user.getId(), new MacrosRequest(2200, 170.0, null, null));
        em.flush();

        assertThat(first).isEqualTo(new Macros(2500, 180, 250, 80));
        assertThat(second).isEqualTo(new Macros(2200, 170, 0, 0));
        long goals = em.getEntityManager()
            .createQuery("select count(g) from NutritionGoal g where g.user.id = :userId", Long.class)
            .setParameter("userId", user.getId())
            .getSingleResult();
        assertThat(goals).isEqualTo(1);
    }

    @Test
    void summaryAddsUpEntriesFromSinceOnwards() {
        em.persist(new NutritionGoal(user, 2500, 180, 250, 80));
        persist(new NutritionEntry(user, 999, 99, 99, 99), "2026-06-11T21:59:59Z");
        persist(new NutritionEntry(user, 500, 40, 50, 15), "2026-06-11T22:00:00Z");
        persist(new NutritionEntry(user, 700, 50, 60, 25), "2026-06-12T08:00:00Z");
        persist(new NutritionEntry(other, 900, 1, 1, 1), "2026-06-12T08:00:00Z");

        NutritionSummaryResponse summary = service.summary(user.getId(), Instant.parse("2026-06-11T22:00:00Z"));

        assertThat(summary).isEqualTo(new NutritionSummaryResponse(
            new Macros(2500, 180, 250, 80), new Macros(1200, 90, 110, 40)));
    }

    @Test
    void summaryWithoutAGoalOrEntriesIsNullAndZeros() {
        assertThat(service.summary(user.getId(), Instant.parse("2026-06-11T22:00:00Z")))
            .isEqualTo(new NutritionSummaryResponse(null, new Macros(0, 0, 0, 0)));
    }

    @Test
    void summaryWithoutSinceStartsAtTheCurrentUtcDay() {
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        persist(new NutritionEntry(user, 300), startOfToday.minusSeconds(1).toString());
        persist(new NutritionEntry(user, 400), startOfToday.toString());

        assertThat(service.summary(user.getId(), null).consumed().calories()).isEqualTo(400);
    }

    private NutritionEntry persist(NutritionEntry entry, String createdAt) {
        entry.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(entry);
    }
}
