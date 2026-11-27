package com.lockedin.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.common.NotImplementedException;

import jakarta.validation.Valid;

/** docs/API.md, "Auth": endpoints 1–2. Both are public. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    /** 1. 409 "An account with this email already exists" if the email is taken. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        throw new NotImplementedException();
    }

    /** 2. 401 "Email or password is incorrect", whichever one is wrong. */
    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        throw new NotImplementedException();
    }
}
