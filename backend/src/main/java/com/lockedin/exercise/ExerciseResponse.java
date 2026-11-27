package com.lockedin.exercise;

import java.time.Instant;
import java.util.UUID;

public record ExerciseResponse(
    UUID id, String name, String description, String muscleGroup, String equipment, Instant createdAt) {
}
