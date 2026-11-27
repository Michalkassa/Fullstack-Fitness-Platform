package com.lockedin.exercise;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.common.NotImplementedException;

/** docs/API.md, "Exercise catalog": endpoint 14. The same read-only library for every user. */
@RestController
@RequestMapping("/api/exercise-catalog")
class ExerciseCatalogController {

    /** 14. Sorted by muscle group, then name. */
    @GetMapping
    List<CatalogExerciseResponse> list() {
        throw new NotImplementedException();
    }
}
