package com.lockedin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create an exercise. {@code muscleGroup} and {@code equipment} are filled in when picked from the library. */
public record ExerciseRequest(
    @NotBlank(message = "Enter a name and a description")
    @Size(max = 40, message = "Name must be at most 40 characters") String name,
    @NotBlank(message = "Enter a name and a description") String description,
    String muscleGroup,
    String equipment) {
}
