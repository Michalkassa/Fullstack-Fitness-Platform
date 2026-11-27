package com.lockedin.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.user.User;

@JpaModelTest
class ExerciseTest {

    @Autowired
    TestEntityManager em;

    private User user() {
        return em.persist(new User("ex@example.com", "hash"));
    }

    @Test
    void persistsAndReadsBackAllFields() {
        User user = user();
        Exercise exercise = new Exercise(user, "Bench Press", "Press the bar.");
        exercise.setMuscleGroup("Chest");
        exercise.setEquipment("Barbell");

        Exercise saved = em.persistFlushFind(exercise);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getName()).isEqualTo("Bench Press");
        assertThat(saved.getDescription()).isEqualTo("Press the bar.");
        assertThat(saved.getMuscleGroup()).isEqualTo("Chest");
        assertThat(saved.getEquipment()).isEqualTo("Barbell");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void muscleGroupAndEquipmentAreOptional() {
        Exercise saved = em.persistFlushFind(new Exercise(user(), "Mystery", "?"));

        assertThat(saved.getMuscleGroup()).isNull();
        assertThat(saved.getEquipment()).isNull();
    }

    @Test
    void storesLongDescriptions() {
        String longText = "x".repeat(5_000);

        Exercise saved = em.persistFlushFind(new Exercise(user(), "Long", longText));

        assertThat(saved.getDescription()).hasSize(5_000);
    }

    @Test
    void namesAreNotUniqueAcrossUsers() {
        em.persist(new Exercise(user(), "Squat", ""));
        User other = em.persist(new User("other@example.com", "hash"));

        Exercise saved = em.persistFlushFind(new Exercise(other, "Squat", ""));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void requiresUserNameAndDescription() {
        User user = new User("a@example.com", "hash");
        assertThatThrownBy(() -> new Exercise(null, "n", "d")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Exercise(user, null, "d")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Exercise(user, "n", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isDeletedWithItsUser() {
        User user = user();
        Exercise exercise = em.persistAndFlush(new Exercise(user, "Row", ""));

        em.remove(user);
        em.flush();
        em.clear();

        assertThat(em.find(Exercise.class, exercise.getId())).isNull();
    }
}
