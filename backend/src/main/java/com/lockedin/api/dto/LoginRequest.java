package com.lockedin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Please enter an email") String email,
    @NotBlank(message = "Please enter a password") String password) {
}
