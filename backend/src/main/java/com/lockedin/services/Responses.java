package com.lockedin.services;

import com.lockedin.api.dto.BodyWeightResponse;
import com.lockedin.api.dto.CatalogExerciseResponse;
import com.lockedin.api.dto.ExerciseResponse;
import com.lockedin.api.dto.LogResponse;
import com.lockedin.api.dto.Macros;
import com.lockedin.api.dto.NutritionEntryResponse;
import com.lockedin.api.dto.UserResponse;
import com.lockedin.api.dto.WorkoutExerciseResponse;
import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.models.BodyWeight;
import com.lockedin.models.Exercise;
import com.lockedin.models.ExerciseCatalog;
import com.lockedin.models.Log;
import com.lockedin.models.NutritionEntry;
import com.lockedin.models.NutritionGoal;
import com.lockedin.models.User;
import com.lockedin.models.Workout;
import com.lockedin.models.WorkoutExercise;

/** Entity → response mapping. Call inside a transaction: some of these touch lazy associations. */
final class Responses {

    private Responses() {
    }

    static UserResponse of(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getImage(), user.getXp());
    }

    static BodyWeightResponse of(BodyWeight entry) {
        return new BodyWeightResponse(entry.getId(), entry.getWeight(), entry.getCreatedAt());
    }

    static ExerciseResponse of(Exercise exercise) {
        return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getDescription(),
            exercise.getMuscleGroup(), exercise.getEquipment(), exercise.getCreatedAt());
    }

    static CatalogExerciseResponse of(ExerciseCatalog entry) {
        return new CatalogExerciseResponse(entry.getId(), entry.getName(), entry.getMuscleGroup(),
            entry.getEquipment(), entry.getInstructions());
    }

    static LogResponse of(Log log) {
        return new LogResponse(log.getId(), log.getExercise().getId(), log.getWeight(), log.getReps(),
            log.getOneRepMax(), log.getCreatedAt());
    }

    static WorkoutResponse of(Workout workout) {
        return new WorkoutResponse(workout.getId(), workout.getName(), workout.getDescription(),
            workout.getCreatedAt(), workout.getExercises().stream().map(Responses::of).toList());
    }

    static WorkoutExerciseResponse of(WorkoutExercise link) {
        return new WorkoutExerciseResponse(link.getId(), link.getExercise().getId(), link.getExercise().getName());
    }

    static NutritionEntryResponse of(NutritionEntry entry) {
        return new NutritionEntryResponse(entry.getId(), entry.getCalories(), entry.getProtein(),
            entry.getCarbs(), entry.getFat(), entry.getCreatedAt());
    }

    static Macros of(NutritionGoal goal) {
        return new Macros(goal.getCalories(), goal.getProtein(), goal.getCarbs(), goal.getFat());
    }
}
