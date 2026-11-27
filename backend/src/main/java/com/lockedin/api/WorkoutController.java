package com.lockedin.api;

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

import jakarta.validation.Valid;

import com.lockedin.api.dto.AddWorkoutExerciseRequest;
import com.lockedin.api.dto.WorkoutExerciseResponse;
import com.lockedin.api.dto.WorkoutRequest;
import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.api.dto.WorkoutSessionRequest;
import com.lockedin.api.dto.WorkoutSessionResult;
import com.lockedin.services.WorkoutService;

/**
 * docs/API.md, "Workouts": endpoints 19–26. Program templates never show up here:
 * a workout that isn't the user's, or is a template, is a 404.
 */
@RestController
@RequestMapping("/api/workouts")
class WorkoutController {

    private final WorkoutService workoutService;

    WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    /** 19. Newest first, each with its exercises (fetched in bulk, not one query per workout). */
    @GetMapping
    List<WorkoutResponse> list(@CurrentUser UUID userId) {
        return workoutService.list(userId);
    }

    /** 20. */
    @GetMapping("/{id}")
    WorkoutResponse get(@CurrentUser UUID userId, @PathVariable UUID id) {
        return workoutService.get(userId, id);
    }

    /** 21. Starts with {@code "exercises": []}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WorkoutResponse create(@CurrentUser UUID userId, @Valid @RequestBody WorkoutRequest request) {
        return workoutService.create(userId, request);
    }

    /** 22. */
    @PutMapping("/{id}")
    WorkoutResponse update(@CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody WorkoutRequest request) {
        return workoutService.update(userId, id, request);
    }

    /** 23. Deletes the workout and its links; the exercises and their logs stay. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        workoutService.delete(userId, id);
    }

    /** 24. 201 with the new link, or 200 with the existing one if the exercise is already in the workout. */
    @PostMapping("/{id}/exercises")
    ResponseEntity<WorkoutExerciseResponse> addExercise(@CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody AddWorkoutExerciseRequest request) {
        WorkoutService.AddedExercise added = workoutService.addExercise(userId, id, request.exerciseId());
        return ResponseEntity.status(added.created() ? HttpStatus.CREATED : HttpStatus.OK).body(added.exercise());
    }

    /** 25. {@code workoutExerciseId} is the link id; 404 if the link isn't part of this workout. */
    @DeleteMapping("/{id}/exercises/{workoutExerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeExercise(@CurrentUser UUID userId, @PathVariable UUID id, @PathVariable UUID workoutExerciseId) {
        workoutService.removeExercise(userId, id, workoutExerciseId);
    }

    /** 26. Saves every set as a log with the same timestamp, in one transaction, and awards XP. */
    @PostMapping("/{id}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    WorkoutSessionResult finishSession(@CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody WorkoutSessionRequest request) {
        return workoutService.finishSession(userId, id, request);
    }
}
