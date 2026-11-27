package com.lockedin.api.dto;

import com.lockedin.models.NutritionGoal;

/** Calories in kcal, macros in grams. Also the shape of {@code NutritionGoal}. */
public record Macros(int calories, double protein, double carbs, double fat) {
}
