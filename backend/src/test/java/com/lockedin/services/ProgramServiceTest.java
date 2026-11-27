package com.lockedin.services;

import static com.lockedin.services.StatusAssertions.assertStatus;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.lockedin.JpaModelTest;
import com.lockedin.api.dto.WorkoutExerciseResponse;
import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.models.Exercise;
import com.lockedin.models.User;
import com.lockedin.models.Workout;

@JpaModelTest
@Import(ProgramService.class)
class ProgramServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProgramService service;

    User coach;
    User user;
    Exercise coachBench;
    Exercise coachSquat;
    Exercise coachRow;
    Workout program;

    @BeforeEach
    void setUp() {
        coach = em.persist(new User("coach@example.com", "hash"));
        user = em.persist(new User("prog-svc@example.com", "hash"));
        coachSquat = exercise(coach, "Squat", "Sit down, stand up", "Legs", "Barbell");
        coachBench = exercise(coach, "Bench", "Press", "Chest", "Barbell");
        coachRow = exercise(coach, "Row", "Pull", "Back", "Barbell");
        program = template("AAA 5x5", "Three lifts", true);
        program.addExercise(coachSquat);
        program.addExercise(coachBench);
        program.addExercise(coachRow);
        // Links are ordered by createdAt, so space them out like the V2 seed does.
        for (int i = 0; i < program.getExercises().size(); i++) {
            program.getExercises().get(i).setCreatedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i));
        }
        em.persistAndFlush(program);
    }

    @Test
    void listReturnsPublicTemplatesByNameWithTheirExercises() {
        Workout hidden = em.persistAndFlush(template("Private", "", false));
        em.persistAndFlush(new Workout(user, "Plain", ""));

        List<WorkoutResponse> programs = service.list();

        assertThat(programs).isSortedAccordingTo(Comparator.comparing(WorkoutResponse::name));
        assertThat(programs).extracting(WorkoutResponse::id).doesNotContain(hidden.getId());
        assertThat(programs).extracting(WorkoutResponse::name).doesNotContain("Plain");
        WorkoutResponse mine = programs.stream().filter(p -> p.id().equals(program.getId())).findFirst().orElseThrow();
        assertThat(mine.name()).isEqualTo("AAA 5x5");
        assertThat(mine.description()).isEqualTo("Three lifts");
        assertThat(mine.exercises()).extracting(WorkoutExerciseResponse::name).containsExactly("Squat", "Bench", "Row");
    }

    @Test
    void listIncludesTheSeededPrograms() {
        assertThat(service.list()).hasSizeGreaterThanOrEqualTo(7).allSatisfy(p -> assertThat(p.exercises()).isNotEmpty());
    }

    @Test
    void copyReusesTheUsersExercisesByNameAndCopiesTheRestInOrder() {
        Exercise ownBench = exercise(user, "Bench", "My bench", null, null);

        WorkoutResponse copy = service.copy(user.getId(), program.getId());
        em.flush();
        em.clear();

        assertThat(copy.id()).isNotEqualTo(program.getId());
        assertThat(copy.name()).isEqualTo("AAA 5x5");
        assertThat(copy.description()).isEqualTo("Three lifts");
        assertThat(copy.exercises()).extracting(WorkoutExerciseResponse::name).containsExactly("Squat", "Bench", "Row");
        assertThat(copy.exercises().get(1).exerciseId()).isEqualTo(ownBench.getId());

        Workout saved = em.find(Workout.class, copy.id());
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.isTemplate()).isFalse();
        assertThat(saved.isPublic()).isFalse();
        assertThat(saved.getExercises()).extracting(link -> link.getExercise().getName())
            .containsExactly("Squat", "Bench", "Row");

        Exercise squatCopy = em.find(Exercise.class, copy.exercises().get(0).exerciseId());
        assertThat(squatCopy.getId()).isNotEqualTo(coachSquat.getId());
        assertThat(squatCopy.getUser().getId()).isEqualTo(user.getId());
        assertThat(squatCopy.getDescription()).isEqualTo("Sit down, stand up");
        assertThat(squatCopy.getMuscleGroup()).isEqualTo("Legs");
        assertThat(squatCopy.getEquipment()).isEqualTo("Barbell");
    }

    @Test
    void copyingTwiceReusesTheExercisesCopiedTheFirstTime() {
        WorkoutResponse first = service.copy(user.getId(), program.getId());
        WorkoutResponse second = service.copy(user.getId(), program.getId());

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.exercises()).extracting(WorkoutExerciseResponse::exerciseId)
            .containsExactlyElementsOf(first.exercises().stream().map(WorkoutExerciseResponse::exerciseId).toList());
    }

    @Test
    void copyingSomethingThatIsNotAPublicTemplateIsNotFound() {
        Workout hidden = em.persistAndFlush(template("Private", "", false));
        Workout plain = em.persistAndFlush(new Workout(coach, "Plain", ""));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.copy(user.getId(), hidden.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.copy(user.getId(), plain.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.copy(user.getId(), UUID.randomUUID()));
    }

    private Exercise exercise(User owner, String name, String description, String muscleGroup, String equipment) {
        Exercise exercise = new Exercise(owner, name, description);
        exercise.setMuscleGroup(muscleGroup);
        exercise.setEquipment(equipment);
        return em.persistAndFlush(exercise);
    }

    private Workout template(String name, String description, boolean isPublic) {
        Workout workout = new Workout(coach, name, description);
        workout.setTemplate(true);
        workout.setPublic(isPublic);
        return workout;
    }
}
