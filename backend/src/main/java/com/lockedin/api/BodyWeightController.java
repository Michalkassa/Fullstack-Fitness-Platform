package com.lockedin.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.api.dto.BodyWeightRequest;
import com.lockedin.api.dto.BodyWeightResponse;
import com.lockedin.services.BodyWeightService;

import jakarta.validation.Valid;

/** docs/API.md, "Body weight": endpoints 5–7. */
@RestController
@RequestMapping("/api/bodyweights")
class BodyWeightController {

    private final BodyWeightService bodyWeightService;

    public BodyWeightController(BodyWeightService bodyWeightService) {
        this.bodyWeightService = bodyWeightService;
    }

    /** 5. Oldest first; {@code []} if there are none. */
    @GetMapping
    List<BodyWeightResponse> list(@CurrentUser UUID userId) {
        return bodyWeightService.list(userId);
    }

    /** 6. Recorded with the current time. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BodyWeightResponse create(@CurrentUser UUID userId, @Valid @RequestBody BodyWeightRequest request) {
        return bodyWeightService.create(userId, request);
    }

    /** 7. 404 if it doesn't exist or isn't the user's. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        bodyWeightService.delete(userId, id);
    }
}
