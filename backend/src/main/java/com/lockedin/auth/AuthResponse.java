package com.lockedin.auth;

import com.lockedin.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
