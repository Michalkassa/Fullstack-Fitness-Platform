package com.lockedin.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lockedin.api.dto.ExerciseRequest;
import com.lockedin.api.dto.ExerciseResponse;
import com.lockedin.api.dto.ExerciseUpdateRequest;
import com.lockedin.api.dto.PopularExerciseResponse;
import com.lockedin.models.Exercise;
import com.lockedin.persistence.repositories.ExerciseRepository;
import com.lockedin.persistence.repositories.UserRepository;

@Service
@Transactional
public class ExerciseService {

    private final ExerciseRepository exercises;
    private final UserRepository users;

    public ExerciseService(ExerciseRepository exercises, UserRepository users) {
        this.exercises = exercises;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> list(UUID userId) {
        return exercises.findByUserIdOrderByCreatedAtDesc(userId).stream().map(Responses::of).toList();
    }

    @Transactional(readOnly = true)
    public List<PopularExerciseResponse> popular(UUID userId, int limit) {
        return exercises.findPopular(userId, Limit.of(limit)).stream()
            .map(row -> new PopularExerciseResponse(row.id(), row.name(), Math.toIntExact(row.logCount())))
            .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseResponse get(UUID userId, UUID id) {
        return Responses.of(find(userId, id));
    }

    public ExerciseResponse create(UUID userId, ExerciseRequest request) {
        Exercise exercise = new Exercise(users.getReferenceById(userId),
            request.name().trim(), request.description().trim());
        exercise.setMuscleGroup(request.muscleGroup());
        exercise.setEquipment(request.equipment());
        return Responses.of(exercises.save(exercise));
    }

    public ExerciseResponse update(UUID userId, UUID id, ExerciseUpdateRequest request) {
        Exercise exercise = find(userId, id);
        exercise.setName(request.name().trim());
        exercise.setDescription(request.description().trim());
        return Responses.of(exercise);
    }

    /** The database cascades to the exercise's logs and workout links. */
    public void delete(UUID userId, UUID id) {
        exercises.delete(find(userId, id));
    }

    private Exercise find(UUID userId, UUID id) {
        return exercises.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Exercise not found"));
    }
}
