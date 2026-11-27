package com.lockedin.api.dto;

public record AuthResponse(String token, UserResponse user) {
}
