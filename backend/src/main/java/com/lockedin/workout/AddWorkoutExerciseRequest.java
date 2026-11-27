package com.lockedin.workout;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddWorkoutExerciseRequest(
    @NotNull(message = "Choose an exercise") UUID exerciseId) {
}
