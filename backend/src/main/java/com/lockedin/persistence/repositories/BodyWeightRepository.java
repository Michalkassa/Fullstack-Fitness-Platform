package com.lockedin.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lockedin.models.BodyWeight;

public interface BodyWeightRepository extends JpaRepository<BodyWeight, UUID> {

    List<BodyWeight> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<BodyWeight> findByIdAndUserId(UUID id, UUID userId);
}
