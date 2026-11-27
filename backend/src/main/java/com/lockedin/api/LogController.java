package com.lockedin.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.lockedin.api.dto.LogRequest;
import com.lockedin.api.dto.LogResponse;
import com.lockedin.services.LogService;

/** docs/API.md, "Logs": endpoints 15–18. An exercise or log that isn't the user's is a 404. */
@RestController
class LogController {

    private final LogService logService;

    LogController(LogService logService) {
        this.logService = logService;
    }

    /** 15. Oldest first. */
    @GetMapping("/api/exercises/{exerciseId}/logs")
    List<LogResponse> list(@CurrentUser UUID userId, @PathVariable UUID exerciseId) {
        return logService.list(userId, exerciseId);
    }

    /** 16. The most recent {@code limit} sets, newest first. */
    @GetMapping("/api/exercises/{exerciseId}/logs/previous")
    List<LogResponse> previous(@CurrentUser UUID userId, @PathVariable UUID exerciseId, @RequestParam int limit) {
        return logService.previous(userId, exerciseId, limit);
    }

    /** 17. Calculates {@code oneRepMax} and awards {@code Xp.PER_SET}. */
    @PostMapping("/api/exercises/{exerciseId}/logs")
    @ResponseStatus(HttpStatus.CREATED)
    LogResponse create(@CurrentUser UUID userId, @PathVariable UUID exerciseId,
            @Valid @RequestBody LogRequest request) {
        return logService.create(userId, exerciseId, request);
    }

    /** 18. Awarded XP isn't taken back. */
    @DeleteMapping("/api/logs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        logService.delete(userId, id);
    }
}
