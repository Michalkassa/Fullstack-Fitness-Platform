package com.lockedin.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BodyWeightRequest(
    @NotNull(message = "Please enter a weight")
    @Positive(message = "Please enter a weight") Double weight) {
}
