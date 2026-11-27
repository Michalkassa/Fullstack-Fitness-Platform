package com.lockedin.exercise;

import java.util.UUID;

public record PopularExerciseResponse(UUID id, String name, int logCount) {
}
