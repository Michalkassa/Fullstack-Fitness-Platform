package com.lockedin.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.models.Exercise;
import com.lockedin.models.User;
import com.lockedin.models.Workout;
import com.lockedin.models.WorkoutExercise;
import com.lockedin.persistence.repositories.ExerciseRepository;
import com.lockedin.persistence.repositories.UserRepository;
import com.lockedin.persistence.repositories.WorkoutRepository;

/** Programs are public workout templates (seeded by Flyway V2) that users copy into their own workouts. */
@Service
@Transactional
public class ProgramService {

    private final WorkoutRepository workouts;
    private final ExerciseRepository exercises;
    private final UserRepository users;

    public ProgramService(WorkoutRepository workouts, ExerciseRepository exercises, UserRepository users) {
        this.workouts = workouts;
        this.exercises = exercises;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<WorkoutResponse> list() {
        return workouts.findByIsTemplateTrueAndIsPublicTrueOrderByNameAsc().stream().map(Responses::of).toList();
    }

    /**
     * Creates a workout for the user with the program's name, description and exercises in order.
     * Each exercise is the user's own one with the same name if they have it, otherwise a copy.
     */
    public WorkoutResponse copy(UUID userId, UUID programId) {
        Workout program = workouts.findByIdAndIsTemplateTrueAndIsPublicTrue(programId)
            .orElseThrow(() -> new NotFoundException("Program not found"));
        User user = users.getReferenceById(userId);

        Workout copy = new Workout(user, program.getName(), program.getDescription());
        for (WorkoutExercise link : program.getExercises()) {
            copy.addExercise(ownExercise(user, link.getExercise()));
        }
        return Responses.of(workouts.save(copy));
    }

    private Exercise ownExercise(User user, Exercise source) {
        return exercises.findFirstByUserIdAndNameOrderByCreatedAtAsc(user.getId(), source.getName())
            .orElseGet(() -> {
                Exercise exercise = new Exercise(user, source.getName(), source.getDescription());
                exercise.setMuscleGroup(source.getMuscleGroup());
                exercise.setEquipment(source.getEquipment());
                return exercises.save(exercise);
            });
    }
}
