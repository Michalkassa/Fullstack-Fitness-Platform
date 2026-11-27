package com.lockedin.bodyweight;

import java.time.Instant;
import java.util.UUID;

public record BodyWeightResponse(UUID id, double weight, Instant createdAt) {
}
