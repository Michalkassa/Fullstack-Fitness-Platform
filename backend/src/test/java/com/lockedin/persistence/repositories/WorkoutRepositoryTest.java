package com.lockedin.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.Persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.models.BaseEntity;
import com.lockedin.models.Exercise;
import com.lockedin.models.User;
import com.lockedin.models.Workout;

@JpaModelTest
class WorkoutRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    WorkoutRepository workouts;

    User user;
    User other;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("wo-repo@example.com", "hash"));
        other = em.persist(new User("wo-repo-other@example.com", "hash"));
    }

    @Test
    void listsTheUsersWorkoutsNewestFirstWithoutTemplates() {
        Workout older = persist(new Workout(user, "Pull", ""), "2026-06-01T08:00:00Z");
        Workout newer = persist(new Workout(user, "Push", ""), "2026-06-02T08:00:00Z");
        persist(template(user, "My template", false), "2026-06-03T08:00:00Z");
        persist(new Workout(other, "Legs", ""), "2026-06-03T08:00:00Z");

        assertThat(workouts.findByUserIdAndIsTemplateFalseOrderByCreatedAtDesc(user.getId()))
            .extracting(BaseEntity::getId)
            .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void listingFetchesTheExercisesAndTheirDetails() {
        Exercise bench = em.persist(new Exercise(user, "Bench", ""));
        Workout push = new Workout(user, "Push", "");
        push.addExercise(bench);
        persist(push, "2026-06-01T08:00:00Z");
        em.clear();

        List<Workout> found = workouts.findByUserIdAndIsTemplateFalseOrderByCreatedAtDesc(user.getId());

        var util = Persistence.getPersistenceUtil();
        assertThat(util.isLoaded(found.get(0), "exercises")).isTrue();
        assertThat(util.isLoaded(found.get(0).getExercises().get(0).getExercise(), "name")).isTrue();
    }

    @Test
    void findsAWorkoutOnlyForItsOwnerAndNeverATemplate() {
        Workout push = persist(new Workout(user, "Push", ""), "2026-06-01T08:00:00Z");
        Workout tmpl = persist(template(user, "Template", false), "2026-06-01T08:00:00Z");

        assertThat(workouts.findByIdAndUserIdAndIsTemplateFalse(push.getId(), user.getId())).isPresent();
        assertThat(workouts.findByIdAndUserIdAndIsTemplateFalse(push.getId(), other.getId())).isEmpty();
        assertThat(workouts.findByIdAndUserIdAndIsTemplateFalse(tmpl.getId(), user.getId())).isEmpty();
    }

    @Test
    void listsPublicTemplatesByName() {
        Workout mine = persist(template(user, "AAA Program", true), "2026-06-01T08:00:00Z");
        Workout hidden = persist(template(user, "Private", false), "2026-06-01T08:00:00Z");
        Workout plain = persist(new Workout(user, "Plain", ""), "2026-06-01T08:00:00Z");

        List<Workout> programs = workouts.findByIsTemplateTrueAndIsPublicTrueOrderByNameAsc();

        assertThat(programs).extracting(BaseEntity::getId)
            .contains(mine.getId())
            .doesNotContain(hidden.getId(), plain.getId());
        assertThat(programs).isSortedAccordingTo(Comparator.comparing(Workout::getName));
        assertThat(programs).allSatisfy(w -> assertThat(w.isTemplate() && w.isPublic()).isTrue());
    }

    @Test
    void findsOnlyPublicTemplatesAsPrograms() {
        Workout program = persist(template(user, "Program", true), "2026-06-01T08:00:00Z");
        Workout hidden = persist(template(user, "Private", false), "2026-06-01T08:00:00Z");
        Workout plain = persist(new Workout(user, "Plain", ""), "2026-06-01T08:00:00Z");

        assertThat(workouts.findByIdAndIsTemplateTrueAndIsPublicTrue(program.getId())).isPresent();
        assertThat(workouts.findByIdAndIsTemplateTrueAndIsPublicTrue(hidden.getId())).isEmpty();
        assertThat(workouts.findByIdAndIsTemplateTrueAndIsPublicTrue(plain.getId())).isEmpty();
    }

    private static Workout template(User owner, String name, boolean isPublic) {
        Workout workout = new Workout(owner, name, "");
        workout.setTemplate(true);
        workout.setPublic(isPublic);
        return workout;
    }

    private Workout persist(Workout workout, String createdAt) {
        workout.setCreatedAt(Instant.parse(createdAt));
        return em.persistAndFlush(workout);
    }
}
