package com.lockedin.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.lockedin.api.dto.AuthResponse;
import com.lockedin.api.dto.LoginRequest;
import com.lockedin.api.dto.RegisterRequest;
import com.lockedin.api.dto.UserResponse;
import com.lockedin.services.TokenService;
import com.lockedin.services.UserService;

/** docs/API.md, "Auth": endpoints 1–2. Both are public. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final UserService users;
    private final TokenService tokens;

    AuthController(UserService users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    /** 1. 409 "An account with this email already exists" if the email is taken. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return signedIn(users.register(request));
    }

    /** 2. 401 "Email or password is incorrect", whichever one is wrong. */
    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return signedIn(users.authenticate(request));
    }

    private AuthResponse signedIn(UserResponse user) {
        return new AuthResponse(tokens.issue(user.id()), user);
    }
}
