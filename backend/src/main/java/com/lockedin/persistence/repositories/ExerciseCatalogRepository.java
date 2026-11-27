package com.lockedin.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lockedin.models.ExerciseCatalog;

public interface ExerciseCatalogRepository extends JpaRepository<ExerciseCatalog, UUID> {

    List<ExerciseCatalog> findAllByOrderByMuscleGroupAscNameAsc();
}
