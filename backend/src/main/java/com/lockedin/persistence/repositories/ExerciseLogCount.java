package com.lockedin.persistence.repositories;

import java.util.UUID;

/** One row of {@code GET /api/exercises/popular}. */
public record ExerciseLogCount(UUID id, String name, long logCount) {
}
