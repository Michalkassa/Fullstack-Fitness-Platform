package com.lockedin.user;

import java.util.UUID;

/** The signed-in user. Never includes the password hash. */
public record UserResponse(UUID id, String email, String name, String image, int xp) {
}
