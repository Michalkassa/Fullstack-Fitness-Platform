package com.lockedin.log;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** One set: weight × reps. Also used for each set in a workout session. Weight 0 is a bodyweight set. */
public record LogRequest(
    @NotNull(message = "Enter a weight and the reps")
    @PositiveOrZero(message = "Enter a weight and the reps") Double weight,
    @NotNull(message = "Enter a weight and the reps")
    @Positive(message = "Enter a weight and the reps")
    @Max(value = OneRepMax.MAX_REPS, message = "At most 36 reps per set") Integer reps) {
}
