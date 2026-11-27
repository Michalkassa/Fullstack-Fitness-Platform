package com.lockedin.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lockedin.models.Exercise;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Exercise> findByIdAndUserId(UUID id, UUID userId);

    /** Names aren't unique, so program copy reuses the user's oldest exercise with that name. */
    Optional<Exercise> findFirstByUserIdAndNameOrderByCreatedAtAsc(UUID userId, String name);

    /** The user's exercises by number of logs, highest first; exercises with no logs come last. */
    @Query("""
        select new com.lockedin.persistence.repositories.ExerciseLogCount(e.id, e.name, count(l))
        from Exercise e left join Log l on l.exercise = e
        where e.user.id = :userId
        group by e.id, e.name
        order by count(l) desc, e.name asc
        """)
    List<ExerciseLogCount> findPopular(UUID userId, Limit limit);
}
