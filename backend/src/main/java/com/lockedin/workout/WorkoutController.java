package com.lockedin.workout;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.common.NotImplementedException;

import jakarta.validation.Valid;

/**
 * docs/API.md, "Workouts": endpoints 19–26. Program templates never show up here:
 * a workout that isn't the user's, or is a template, is a 404.
 */
@RestController
@RequestMapping("/api/workouts")
class WorkoutController {

    /** 19. Newest first, each with its exercises (fetch them in bulk, not one query per workout). */
    @GetMapping
    List<WorkoutResponse> list() {
        throw new NotImplementedException();
    }

    /** 20. */
    @GetMapping("/{id}")
    WorkoutResponse get(@PathVariable UUID id) {
        throw new NotImplementedException();
    }

    /** 21. Starts with {@code "exercises": []}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WorkoutResponse create(@Valid @RequestBody WorkoutRequest request) {
        throw new NotImplementedException();
    }

    /** 22. */
    @PutMapping("/{id}")
    WorkoutResponse update(@PathVariable UUID id, @Valid @RequestBody WorkoutRequest request) {
        throw new NotImplementedException();
    }

    /** 23. Deletes the workout and its links; the exercises and their logs stay. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        throw new NotImplementedException();
    }

    /** 24. 201 with the new link, or 200 with the existing one if the exercise is already in the workout. */
    @PostMapping("/{id}/exercises")
    ResponseEntity<WorkoutExerciseResponse> addExercise(
            @PathVariable UUID id, @Valid @RequestBody AddWorkoutExerciseRequest request) {
        throw new NotImplementedException();
    }

    /** 25. {@code workoutExerciseId} is the link id; 404 if the link isn't part of this workout. */
    @DeleteMapping("/{id}/exercises/{workoutExerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeExercise(@PathVariable UUID id, @PathVariable UUID workoutExerciseId) {
        throw new NotImplementedException();
    }

    /** 26. Saves every set as a log with the same timestamp, in one transaction, and awards XP. */
    @PostMapping("/{id}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    WorkoutSessionResult finishSession(@PathVariable UUID id, @Valid @RequestBody WorkoutSessionRequest request) {
        throw new NotImplementedException();
    }
}
