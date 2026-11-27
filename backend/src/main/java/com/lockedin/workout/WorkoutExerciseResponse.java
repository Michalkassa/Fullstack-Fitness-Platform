package com.lockedin.workout;

import java.util.UUID;

/** {@code id} is the link id (used to remove it from the workout); {@code exerciseId} is the exercise. */
public record WorkoutExerciseResponse(UUID id, UUID exerciseId, String name) {
}
