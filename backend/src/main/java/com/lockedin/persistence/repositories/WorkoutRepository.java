package com.lockedin.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lockedin.models.Workout;

/** Queries that return workouts fetch their exercises too, so a list doesn't run one query per workout. */
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    List<Workout> findByUserIdAndIsTemplateFalseOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Workout> findByIdAndUserIdAndIsTemplateFalse(UUID id, UUID userId);

    /** Programs: the public templates. */
    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    List<Workout> findByIsTemplateTrueAndIsPublicTrueOrderByNameAsc();

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Workout> findByIdAndIsTemplateTrueAndIsPublicTrue(UUID id);
}
