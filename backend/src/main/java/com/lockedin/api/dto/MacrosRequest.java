package com.lockedin.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** A nutrition entry or the daily goal. Missing macros count as 0. */
public record MacrosRequest(
    @NotNull(message = "Enter a calorie amount")
    @PositiveOrZero(message = "Calories cannot be negative") Integer calories,
    @PositiveOrZero(message = "Macros cannot be negative") Double protein,
    @PositiveOrZero(message = "Macros cannot be negative") Double carbs,
    @PositiveOrZero(message = "Macros cannot be negative") Double fat) {
}
