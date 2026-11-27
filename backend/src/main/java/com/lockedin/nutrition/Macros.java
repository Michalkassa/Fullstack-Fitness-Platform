package com.lockedin.nutrition;

/** Calories in kcal, macros in grams. Also the shape of {@code NutritionGoal}. */
public record Macros(int calories, double protein, double carbs, double fat) {
}
