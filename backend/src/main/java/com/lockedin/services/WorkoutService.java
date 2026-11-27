package com.lockedin.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lockedin.api.dto.LogRequest;
import com.lockedin.api.dto.WorkoutExerciseResponse;
import com.lockedin.api.dto.WorkoutRequest;
import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.api.dto.WorkoutSessionRequest;
import com.lockedin.api.dto.WorkoutSessionRequest.SessionExercise;
import com.lockedin.api.dto.WorkoutSessionResult;
import com.lockedin.models.Exercise;
import com.lockedin.models.Log;
import com.lockedin.models.OneRepMax;
import com.lockedin.models.Workout;
import com.lockedin.models.WorkoutExercise;
import com.lockedin.models.Xp;
import com.lockedin.persistence.repositories.ExerciseRepository;
import com.lockedin.persistence.repositories.LogRepository;
import com.lockedin.persistence.repositories.UserRepository;
import com.lockedin.persistence.repositories.WorkoutExerciseRepository;
import com.lockedin.persistence.repositories.WorkoutRepository;

/** The user's own workouts. Program templates are never visible here: they're a 404. */
@Service
@Transactional
public class WorkoutService {

    /** The link, and whether this call created it (201) or it was already there (200). */
    public record AddedExercise(WorkoutExerciseResponse exercise, boolean created) {
    }

    private final WorkoutRepository workouts;
    private final WorkoutExerciseRepository links;
    private final ExerciseRepository exercises;
    private final LogRepository logs;
    private final UserRepository users;

    public WorkoutService(WorkoutRepository workouts, WorkoutExerciseRepository links, ExerciseRepository exercises,
            LogRepository logs, UserRepository users) {
        this.workouts = workouts;
        this.links = links;
        this.exercises = exercises;
        this.logs = logs;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<WorkoutResponse> list(UUID userId) {
        return workouts.findByUserIdAndIsTemplateFalseOrderByCreatedAtDesc(userId).stream().map(Responses::of).toList();
    }

    @Transactional(readOnly = true)
    public WorkoutResponse get(UUID userId, UUID id) {
        return Responses.of(find(userId, id));
    }

    public WorkoutResponse create(UUID userId, WorkoutRequest request) {
        Workout workout = new Workout(users.getReferenceById(userId),
            request.name().trim(), request.description().trim());
        return Responses.of(workouts.save(workout));
    }

    public WorkoutResponse update(UUID userId, UUID id, WorkoutRequest request) {
        Workout workout = find(userId, id);
        workout.setName(request.name().trim());
        workout.setDescription(request.description().trim());
        return Responses.of(workout);
    }

    /** Deletes the workout and its links; the exercises and their logs stay. */
    public void delete(UUID userId, UUID id) {
        workouts.delete(find(userId, id));
    }

    public AddedExercise addExercise(UUID userId, UUID workoutId, UUID exerciseId) {
        Workout workout = find(userId, workoutId);
        Exercise exercise = findExercise(userId, exerciseId);
        boolean created = workout.addExercise(exercise);
        workouts.flush();
        WorkoutExercise link = links.findByWorkoutIdAndExerciseId(workout.getId(), exercise.getId()).orElseThrow();
        return new AddedExercise(Responses.of(link), created);
    }

    /** {@code linkId} is a {@code workout_exercises} id; it must belong to this workout. */
    public void removeExercise(UUID userId, UUID workoutId, UUID linkId) {
        Workout workout = find(userId, workoutId);
        WorkoutExercise link = links.findByIdAndWorkoutId(linkId, workout.getId())
            .orElseThrow(() -> new NotFoundException("That exercise isn't in this workout"));
        workout.removeExercise(link.getExercise());
    }

    /**
     * Saves every set as a log with one shared timestamp and awards XP, all or nothing:
     * every exercise is checked before anything is saved.
     */
    public WorkoutSessionResult finishSession(UUID userId, UUID workoutId, WorkoutSessionRequest request) {
        Workout workout = find(userId, workoutId);
        List<Exercise> sessionExercises = new ArrayList<>();
        for (SessionExercise entry : request.exercises()) {
            sessionExercises.add(findExercise(userId, entry.exerciseId()));
        }

        Instant at = Instant.now().truncatedTo(ChronoUnit.MICROS);
        List<Log> created = new ArrayList<>();
        for (int i = 0; i < sessionExercises.size(); i++) {
            Exercise exercise = sessionExercises.get(i);
            for (LogRequest set : request.exercises().get(i).sets()) {
                Log log = new Log(workout.getUser(), exercise, set.weight(), set.reps(),
                    OneRepMax.estimate(set.weight(), set.reps()));
                log.setCreatedAt(at);
                created.add(log);
            }
        }
        logs.saveAll(created);

        int xp = created.size() * Xp.PER_SET + Xp.PER_WORKOUT;
        workout.getUser().addXp(xp);
        return new WorkoutSessionResult(created.size(), xp);
    }

    private Workout find(UUID userId, UUID id) {
        return workouts.findByIdAndUserIdAndIsTemplateFalse(id, userId)
            .orElseThrow(() -> new NotFoundException("Workout not found"));
    }

    private Exercise findExercise(UUID userId, UUID id) {
        return exercises.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Exercise not found"));
    }
}
