package com.lockedin.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Limit;

import com.lockedin.JpaModelTest;
import com.lockedin.models.BaseEntity;
import com.lockedin.models.Exercise;
import com.lockedin.models.Log;
import com.lockedin.models.User;

@JpaModelTest
class LogRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    LogRepository logs;

    User user;
    User other;
    Exercise squat;
    Exercise bench;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("log-repo@example.com", "hash"));
        other = em.persist(new User("log-repo-other@example.com", "hash"));
        squat = em.persist(new Exercise(user, "Squat", ""));
        bench = em.persist(new Exercise(user, "Bench", ""));
    }

    @Test
    void listsAnExercisesLogsOldestFirst() {
        Log second = persist(new Log(user, squat, 105, 5, 118), "2026-06-02T08:00:00Z");
        Log first = persist(new Log(user, squat, 100, 5, 112), "2026-06-01T08:00:00Z");
        persist(new Log(user, bench, 80, 5, 90), "2026-06-01T09:00:00Z");

        assertThat(logs.findByExerciseIdOrderByCreatedAtAsc(squat.getId()))
            .extracting(BaseEntity::getId)
            .containsExactly(first.getId(), second.getId());
    }

    @Test
    void returnsTheMostRecentSetsNewestFirst() {
        persist(new Log(user, squat, 100, 5, 112), "2026-06-01T08:00:00Z");
        Log middle = persist(new Log(user, squat, 105, 5, 118), "2026-06-02T08:00:00Z");
        Log newest = persist(new Log(user, squat, 110, 5, 124), "2026-06-03T08:00:00Z");
        persist(new Log(user, bench, 80, 5, 90), "2026-06-04T08:00:00Z");

        assertThat(logs.findByExerciseIdOrderByCreatedAtDesc(squat.getId(), Limit.of(2)))
            .extracting(BaseEntity::getId)
            .containsExactly(newest.getId(), middle.getId());
    }

    @Test
    void listsAllOfTheUsersLogsOldestFirst() {
        Log later = persist(new Log(user, bench, 80, 5, 90), "2026-06-02T08:00:00Z");
        Log earlier = persist(new Log(user, squat, 100, 5, 112), "2026-06-01T08:00:00Z");
        Exercise othersSquat = em.persist(new Exercise(other, "Squat", ""));
        persist(new Log(other, othersSquat, 100, 5, 112), "2026-06-01T09:00:00Z");

        assertThat(logs.findByUserIdOrderByCreatedAtAsc(user.getId()))
            .extracting(BaseEntity::getId)
            .containsExactly(earlier.getId(), later.getId());
    }

    @Test
    void findsALogOnlyForItsOwner() {
        Log log = persist(new Log(user, squat, 100, 5, 112), "2026-06-01T08:00:00Z");

        assertThat(logs.findByIdAndUserId(log.getId(), user.getId())).isPresent();
        assertThat(logs.findByIdAndUserId(log.getId(), other.getId())).isEmpty();
    }

    private Log persist(Log log, String createdAt) {
        log.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(log);
    }
}
