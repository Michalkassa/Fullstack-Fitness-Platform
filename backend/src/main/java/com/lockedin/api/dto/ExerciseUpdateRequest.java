package com.lockedin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Rename or redescribe an exercise. The frontend always sends both fields. */
public record ExerciseUpdateRequest(
    @NotBlank(message = "Enter a name and a description")
    @Size(max = 40, message = "Name must be at most 40 characters") String name,
    @NotBlank(message = "Enter a name and a description") String description) {
}
