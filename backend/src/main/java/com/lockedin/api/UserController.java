package com.lockedin.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.api.dto.UserResponse;
import com.lockedin.api.dto.UserStatsResponse;

/** docs/API.md, "Users": endpoints 3–4. */
@RestController
@RequestMapping("/api/users/me")
class UserController {

    /** 3. The signed-in user; 401 if the token is missing or invalid. */
    @GetMapping
    UserResponse me() {
        throw new NotImplementedException();
    }

    /** 4. Level and progress toward the next level. */
    @GetMapping("/stats")
    UserStatsResponse stats() {
        throw new NotImplementedException();
    }
}
