package com.lockedin.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lockedin.models.WorkoutExercise;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, UUID> {

    Optional<WorkoutExercise> findByWorkoutIdAndExerciseId(UUID workoutId, UUID exerciseId);

    Optional<WorkoutExercise> findByIdAndWorkoutId(UUID id, UUID workoutId);
}
