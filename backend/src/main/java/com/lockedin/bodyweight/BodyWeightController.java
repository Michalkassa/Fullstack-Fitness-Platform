package com.lockedin.bodyweight;

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

import com.lockedin.common.NotImplementedException;

import jakarta.validation.Valid;

/** docs/API.md, "Body weight": endpoints 5–7. */
@RestController
@RequestMapping("/api/bodyweights")
class BodyWeightController {

    /** 5. Oldest first; {@code []} if there are none. */
    @GetMapping
    List<BodyWeightResponse> list() {
        throw new NotImplementedException();
    }

    /** 6. Recorded with the current time. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BodyWeightResponse create(@Valid @RequestBody BodyWeightRequest request) {
        throw new NotImplementedException();
    }

    /** 7. 404 if it doesn't exist or isn't the user's. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        throw new NotImplementedException();
    }
}
