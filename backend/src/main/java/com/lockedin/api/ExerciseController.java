package com.lockedin.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.lockedin.api.dto.ExerciseRequest;
import com.lockedin.api.dto.ExerciseResponse;
import com.lockedin.api.dto.ExerciseUpdateRequest;
import com.lockedin.api.dto.PopularExerciseResponse;

/** docs/API.md, "Exercises": endpoints 8–13. An exercise that isn't the user's is a 404. */
@RestController
@RequestMapping("/api/exercises")
class ExerciseController {

    /** 8. The user's own exercises, newest first. */
    @GetMapping
    List<ExerciseResponse> list() {
        throw new NotImplementedException();
    }

    /** 9. Most-logged first; exercises with 0 logs may come last. */
    @GetMapping("/popular")
    List<PopularExerciseResponse> popular(@RequestParam(defaultValue = "5") int limit) {
        throw new NotImplementedException();
    }

    /** 10. */
    @GetMapping("/{id}")
    ExerciseResponse get(@PathVariable UUID id) {
        throw new NotImplementedException();
    }

    /** 11. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExerciseResponse create(@Valid @RequestBody ExerciseRequest request) {
        throw new NotImplementedException();
    }

    /** 12. Changes name and description only. */
    @PutMapping("/{id}")
    ExerciseResponse update(@PathVariable UUID id, @Valid @RequestBody ExerciseUpdateRequest request) {
        throw new NotImplementedException();
    }

    /** 13. The database cascades the delete to its logs and workout links. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        throw new NotImplementedException();
    }
}
