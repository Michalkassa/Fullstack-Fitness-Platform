package com.lockedin.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown by endpoints that are mapped but not built yet. The frontend shows
 * "This endpoint isn't implemented yet." for a 501 (docs/API.md, "Errors").
 */
public class NotImplementedException extends ResponseStatusException {

    public NotImplementedException() {
        super(HttpStatus.NOT_IMPLEMENTED, "This endpoint isn't implemented yet");
    }
}
