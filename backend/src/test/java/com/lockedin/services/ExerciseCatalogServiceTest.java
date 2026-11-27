package com.lockedin.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.lockedin.JpaModelTest;
import com.lockedin.api.dto.CatalogExerciseResponse;

@JpaModelTest
@Import(ExerciseCatalogService.class)
class ExerciseCatalogServiceTest {

    @Autowired
    ExerciseCatalogService service;

    @Test
    void listsTheSeededCatalogByMuscleGroupThenName() {
        assertThat(service.list())
            .hasSize(34)
            .isSortedAccordingTo(Comparator.comparing(CatalogExerciseResponse::muscleGroup)
                .thenComparing(CatalogExerciseResponse::name))
            .allSatisfy(entry -> {
                assertThat(entry.id()).isNotNull();
                assertThat(entry.equipment()).isNotBlank();
                assertThat(entry.instructions()).isNotBlank();
            });
    }
}
