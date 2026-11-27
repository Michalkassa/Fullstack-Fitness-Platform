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
import com.lockedin.models.Xp;

/** docs/API.md, "Logs": endpoints 15–18. An exercise or log that isn't the user's is a 404. */
@RestController
class LogController {

    /** 15. Oldest first. */
    @GetMapping("/api/exercises/{exerciseId}/logs")
    List<LogResponse> list(@PathVariable UUID exerciseId) {
        throw new NotImplementedException();
    }

    /** 16. The most recent {@code limit} sets, newest first. */
    @GetMapping("/api/exercises/{exerciseId}/logs/previous")
    List<LogResponse> previous(@PathVariable UUID exerciseId, @RequestParam int limit) {
        throw new NotImplementedException();
    }

    /** 17. Calculates {@code oneRepMax} and awards {@code Xp.PER_SET}. */
    @PostMapping("/api/exercises/{exerciseId}/logs")
    @ResponseStatus(HttpStatus.CREATED)
    LogResponse create(@PathVariable UUID exerciseId, @Valid @RequestBody LogRequest request) {
        throw new NotImplementedException();
    }

    /** 18. Awarded XP isn't taken back. */
    @DeleteMapping("/api/logs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        throw new NotImplementedException();
    }
}
