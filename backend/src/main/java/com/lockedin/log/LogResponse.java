package com.lockedin.log;

import java.time.Instant;
import java.util.UUID;

public record LogResponse(UUID id, UUID exerciseId, double weight, int reps, int oneRepMax, Instant createdAt) {
}
