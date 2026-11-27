package com.lockedin.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lockedin.api.dto.LogRequest;
import com.lockedin.api.dto.LogResponse;
import com.lockedin.models.Exercise;
import com.lockedin.models.Log;
import com.lockedin.models.OneRepMax;
import com.lockedin.models.Xp;
import com.lockedin.persistence.repositories.ExerciseRepository;
import com.lockedin.persistence.repositories.LogRepository;

@Service
@Transactional
public class LogService {

    private final LogRepository logs;
    private final ExerciseRepository exercises;

    public LogService(LogRepository logs, ExerciseRepository exercises) {
        this.logs = logs;
        this.exercises = exercises;
    }

    @Transactional(readOnly = true)
    public List<LogResponse> list(UUID userId, UUID exerciseId) {
        UUID id = findExercise(userId, exerciseId).getId();
        return logs.findByExerciseIdOrderByCreatedAtAsc(id).stream().map(Responses::of).toList();
    }

    /** The last {@code limit} sets, newest first. */
    @Transactional(readOnly = true)
    public List<LogResponse> previous(UUID userId, UUID exerciseId, int limit) {
        UUID id = findExercise(userId, exerciseId).getId();
        return logs.findByExerciseIdOrderByCreatedAtDesc(id, Limit.of(limit)).stream().map(Responses::of).toList();
    }

    /** Calculates the one-rep max and awards {@link Xp#PER_SET}. */
    public LogResponse create(UUID userId, UUID exerciseId, LogRequest request) {
        Exercise exercise = findExercise(userId, exerciseId);
        Log log = logs.save(new Log(exercise.getUser(), exercise, request.weight(), request.reps(),
            OneRepMax.estimate(request.weight(), request.reps())));
        exercise.getUser().addXp(Xp.PER_SET);
        return Responses.of(log);
    }

    /** XP that was already awarded isn't taken back. */
    public void delete(UUID userId, UUID id) {
        logs.delete(logs.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Log not found")));
    }

    private Exercise findExercise(UUID userId, UUID exerciseId) {
        return exercises.findByIdAndUserId(exerciseId, userId)
            .orElseThrow(() -> new NotFoundException("Exercise not found"));
    }
}
