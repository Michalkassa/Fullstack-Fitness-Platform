package com.lockedin.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A workout, or a program template (same shape). {@code exercises} are in the order they were added. */
public record WorkoutResponse(
    UUID id, String name, String description, Instant createdAt, List<WorkoutExerciseResponse> exercises) {
}
