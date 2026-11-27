package com.lockedin.exercise;

import java.util.UUID;

public record CatalogExerciseResponse(UUID id, String name, String muscleGroup, String equipment, String instructions) {
}
