package com.lockedin.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A resource that doesn't exist or isn't the user's: both are a 404 so ids don't leak (docs/API.md). */
public class NotFoundException extends ResponseStatusException {

    public NotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
