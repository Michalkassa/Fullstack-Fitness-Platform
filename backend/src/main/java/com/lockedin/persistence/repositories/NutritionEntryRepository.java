package com.lockedin.persistence.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lockedin.models.NutritionEntry;

public interface NutritionEntryRepository extends JpaRepository<NutritionEntry, UUID> {

    List<NutritionEntry> findByUserIdOrderByCreatedAtAsc(UUID userId);

    /** Entries for the nutrition summary: {@code createdAt >= since}. */
    List<NutritionEntry> findByUserIdAndCreatedAtGreaterThanEqual(UUID userId, Instant since);

    Optional<NutritionEntry> findByIdAndUserId(UUID id, UUID userId);
}
