package com.lockedin.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lockedin.api.dto.CatalogExerciseResponse;
import com.lockedin.persistence.repositories.ExerciseCatalogRepository;

@Service
@Transactional(readOnly = true)
public class ExerciseCatalogService {

    private final ExerciseCatalogRepository catalog;

    public ExerciseCatalogService(ExerciseCatalogRepository catalog) {
        this.catalog = catalog;
    }

    public List<CatalogExerciseResponse> list() {
        return catalog.findAllByOrderByMuscleGroupAscNameAsc().stream().map(Responses::of).toList();
    }
}
