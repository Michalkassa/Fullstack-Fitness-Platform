package com.lockedin.workout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create or update a workout. The frontend always sends both fields. */
public record WorkoutRequest(
    @NotBlank(message = "Enter a name and a description")
    @Size(max = 40, message = "Name must be at most 40 characters") String name,
    @NotBlank(message = "Enter a name and a description") String description) {
}
