package com.lockedin.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.api.dto.UserResponse;
import com.lockedin.api.dto.UserStatsResponse;
import com.lockedin.services.UserService;

/** docs/API.md, "Users": endpoints 3–4. */
@RestController
@RequestMapping("/api/users/me")
class UserController {

    private final UserService users;

    UserController(UserService users) {
        this.users = users;
    }

    /** 3. The signed-in user; 401 if the token is missing or invalid, or the user no longer exists. */
    @GetMapping
    UserResponse me(@CurrentUser UUID userId) {
        return users.get(userId);
    }

    /** 4. Level and progress toward the next level. */
    @GetMapping("/stats")
    UserStatsResponse stats(@CurrentUser UUID userId) {
        return users.stats(userId);
    }
}
