package com.lockedin.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddWorkoutExerciseRequest(
    @NotNull(message = "Choose an exercise") UUID exerciseId) {
}
