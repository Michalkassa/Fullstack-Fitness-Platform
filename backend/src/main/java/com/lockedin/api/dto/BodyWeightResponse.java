package com.lockedin.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BodyWeightResponse(UUID id, double weight, Instant createdAt) {
}
