package com.lockedin.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.api.dto.CatalogExerciseResponse;
import com.lockedin.services.ExerciseCatalogService;

/** docs/API.md, "Exercise catalog": endpoint 14. The same read-only library for every user. */
@RestController
@RequestMapping("/api/exercise-catalog")
class ExerciseCatalogController {

    private final ExerciseCatalogService exerciseCatalogService;

    public ExerciseCatalogController(ExerciseCatalogService exerciseCatalogService){
        this.exerciseCatalogService = exerciseCatalogService;
    }
    /** 14. Sorted by muscle group, then name. */
    @GetMapping
    List<CatalogExerciseResponse> list() {
        return exerciseCatalogService.list();
    }
}
