package com.lockedin.services;

import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lockedin.api.dto.LoginRequest;
import com.lockedin.api.dto.RegisterRequest;
import com.lockedin.api.dto.UserResponse;
import com.lockedin.api.dto.UserStatsResponse;
import com.lockedin.models.User;
import com.lockedin.models.Xp;
import com.lockedin.persistence.repositories.UserRepository;

/** Accounts: register, check a password, and read the signed-in user. Issuing tokens is left to the auth layer. */
@Service
@Transactional
public class UserService {

    static final String EMAIL_TAKEN = "An account with this email already exists";
    static final String BAD_CREDENTIALS = "Email or password is incorrect";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, EMAIL_TAKEN);
        }
        return Responses.of(users.save(new User(email, passwordEncoder.encode(request.password()))));
    }

    /** The same 401 whether the email is unknown or the password is wrong. */
    @Transactional(readOnly = true)
    public UserResponse authenticate(LoginRequest request) {
        return users.findByEmail(normalise(request.email()))
            .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
            .map(Responses::of)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS));
    }

    /** A token for a user that no longer exists is a 401, so the frontend signs out. */
    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return Responses.of(find(userId));
    }

    @Transactional(readOnly = true)
    public UserStatsResponse stats(UUID userId) {
        int xp = find(userId).getXp();
        Xp.LevelProgress progress = Xp.levelProgress(xp);
        return new UserStatsResponse(xp, progress.level(), progress.current(), progress.needed(), progress.percent());
    }

    private User find(UUID userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again"));
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
