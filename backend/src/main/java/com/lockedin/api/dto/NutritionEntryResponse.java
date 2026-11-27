package com.lockedin.api.dto;

import java.time.Instant;
import java.util.UUID;

public record NutritionEntryResponse(
    UUID id, int calories, double protein, double carbs, double fat, Instant createdAt) {
}
