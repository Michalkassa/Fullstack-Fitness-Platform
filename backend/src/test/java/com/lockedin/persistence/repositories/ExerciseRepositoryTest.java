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
class ExerciseRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ExerciseRepository exercises;

    User user;
    User other;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("ex@example.com", "hash"));
        other = em.persist(new User("ex-other@example.com", "hash"));
    }

    @Test
    void listsOnlyTheUsersExercisesNewestFirst() {
        Exercise older = persist(new Exercise(user, "Squat", ""), "2026-06-01T08:00:00Z");
        Exercise newer = persist(new Exercise(user, "Bench", ""), "2026-06-02T08:00:00Z");
        persist(new Exercise(other, "Deadlift", ""), "2026-06-03T08:00:00Z");

        assertThat(exercises.findByUserIdOrderByCreatedAtDesc(user.getId()))
            .extracting(BaseEntity::getId)
            .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findsAnExerciseOnlyForItsOwner() {
        Exercise exercise = persist(new Exercise(user, "Squat", ""), "2026-06-01T08:00:00Z");

        assertThat(exercises.findByIdAndUserId(exercise.getId(), user.getId())).isPresent();
        assertThat(exercises.findByIdAndUserId(exercise.getId(), other.getId())).isEmpty();
    }

    @Test
    void findsTheUsersOldestExerciseWithAName() {
        Exercise first = persist(new Exercise(user, "Squat", ""), "2026-06-01T08:00:00Z");
        persist(new Exercise(user, "Squat", ""), "2026-06-02T08:00:00Z");
        persist(new Exercise(other, "Bench", ""), "2026-06-01T08:00:00Z");

        assertThat(exercises.findFirstByUserIdAndNameOrderByCreatedAtAsc(user.getId(), "Squat"))
            .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(first.getId()));
        assertThat(exercises.findFirstByUserIdAndNameOrderByCreatedAtAsc(user.getId(), "Bench")).isEmpty();
    }

    @Test
    void ranksTheUsersExercisesByLogCountWithUnloggedOnesLast() {
        Exercise squat = persist(new Exercise(user, "Squat", ""), "2026-06-01T08:00:00Z");
        Exercise bench = persist(new Exercise(user, "Bench", ""), "2026-06-01T09:00:00Z");
        Exercise curl = persist(new Exercise(user, "Curl", ""), "2026-06-01T10:00:00Z");
        Exercise othersSquat = persist(new Exercise(other, "Squat", ""), "2026-06-01T08:00:00Z");
        logs(user, squat, 1);
        logs(user, bench, 3);
        logs(other, othersSquat, 5);

        assertThat(exercises.findPopular(user.getId(), Limit.of(5))).containsExactly(
            new ExerciseLogCount(bench.getId(), "Bench", 3),
            new ExerciseLogCount(squat.getId(), "Squat", 1),
            new ExerciseLogCount(curl.getId(), "Curl", 0));
    }

    @Test
    void popularHonoursTheLimit() {
        Exercise squat = persist(new Exercise(user, "Squat", ""), "2026-06-01T08:00:00Z");
        Exercise bench = persist(new Exercise(user, "Bench", ""), "2026-06-01T09:00:00Z");
        logs(user, squat, 2);
        logs(user, bench, 1);

        assertThat(exercises.findPopular(user.getId(), Limit.of(1)))
            .containsExactly(new ExerciseLogCount(squat.getId(), "Squat", 2));
    }

    private Exercise persist(Exercise exercise, String createdAt) {
        exercise.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(exercise);
    }

    private void logs(User owner, Exercise exercise, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(new Log(owner, exercise, 100, 5, 112));
        }
        em.flush();
    }
}
