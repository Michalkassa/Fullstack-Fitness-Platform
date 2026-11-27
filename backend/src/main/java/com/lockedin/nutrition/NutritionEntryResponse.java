package com.lockedin.nutrition;

import java.time.Instant;
import java.util.UUID;

public record NutritionEntryResponse(
    UUID id, int calories, double protein, double carbs, double fat, Instant createdAt) {
}
