package com.lockedin.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lockedin.models.Log;

/** Exercise-scoped queries don't filter by user: check the exercise's owner first. */
public interface LogRepository extends JpaRepository<Log, UUID> {

    List<Log> findByExerciseIdOrderByCreatedAtAsc(UUID exerciseId);

    /** The last {@code limit} sets of an exercise, newest first. */
    List<Log> findByExerciseIdOrderByCreatedAtDesc(UUID exerciseId, Limit limit);

    /** Every log the user has, with its exercise, for analytics and the CSV export. */
    @EntityGraph(attributePaths = "exercise")
    List<Log> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Log> findByIdAndUserId(UUID id, UUID userId);
}
