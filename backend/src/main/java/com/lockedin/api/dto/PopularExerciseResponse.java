package com.lockedin.api.dto;

import java.util.UUID;

public record PopularExerciseResponse(UUID id, String name, int logCount) {
}
