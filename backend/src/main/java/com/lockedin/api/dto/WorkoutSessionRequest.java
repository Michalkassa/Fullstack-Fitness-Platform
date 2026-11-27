package com.lockedin.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.lockedin.models.Log;

/** A finished live workout: every set the user entered, saved all or nothing. */
public record WorkoutSessionRequest(
    @NotEmpty(message = "Log at least one set before finishing") List<@Valid SessionExercise> exercises) {

    public record SessionExercise(
        @NotNull(message = "Choose an exercise") UUID exerciseId,
        @NotEmpty(message = "Log at least one set before finishing") List<@Valid LogRequest> sets) {
    }
}
