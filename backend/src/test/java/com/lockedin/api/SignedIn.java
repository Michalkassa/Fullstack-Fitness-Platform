package com.lockedin.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.UUID;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** A request from a signed-in user, for controller slice tests (real tokens are covered by SecurityRulesTest). */
final class SignedIn {

    static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");

    private SignedIn() {
    }

    static RequestPostProcessor signedIn() {
        return jwt().jwt(j -> j.subject(USER_ID.toString()));
    }
}
