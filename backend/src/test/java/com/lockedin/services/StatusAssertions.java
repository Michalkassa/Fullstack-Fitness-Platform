package com.lockedin.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Services signal API errors with {@link ResponseStatusException}; GlobalExceptionHandler keeps its status and reason. */
final class StatusAssertions {

    private StatusAssertions() {
    }

    static void assertStatus(HttpStatus status, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }

    static void assertStatus(HttpStatus status, String reason, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, e -> {
            assertThat(e.getStatusCode()).isEqualTo(status);
            assertThat(e.getReason()).isEqualTo(reason);
        });
    }
}
