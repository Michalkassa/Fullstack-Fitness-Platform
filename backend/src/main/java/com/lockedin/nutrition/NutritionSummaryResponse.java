package com.lockedin.nutrition;

/** {@code goal} is null if the user hasn't set one; {@code consumed} is all zeros if they've eaten nothing. */
public record NutritionSummaryResponse(Macros goal, Macros consumed) {
}
