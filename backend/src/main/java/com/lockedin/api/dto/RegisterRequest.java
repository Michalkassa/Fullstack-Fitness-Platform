package com.lockedin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Store the email trimmed and lower-cased; hash the password with the {@code PasswordEncoder} bean. */
public record RegisterRequest(
    @NotBlank(message = "Please enter an email")
    @Email(message = "Not a valid email") String email,
    @NotNull(message = "Password must be at least 8 characters")
    @Size(min = 8, message = "Password must be at least 8 characters") String password) {
}
