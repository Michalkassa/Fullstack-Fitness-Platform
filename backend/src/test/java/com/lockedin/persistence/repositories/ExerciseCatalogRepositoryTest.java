package com.lockedin.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.lockedin.JpaModelTest;
import com.lockedin.models.ExerciseCatalog;

@JpaModelTest
class ExerciseCatalogRepositoryTest {

    @Autowired
    ExerciseCatalogRepository catalog;

    @Test
    void listsTheSeededCatalogByMuscleGroupThenName() {
        assertThat(catalog.findAllByOrderByMuscleGroupAscNameAsc())
            .hasSize(34)
            .isSortedAccordingTo(Comparator.comparing(ExerciseCatalog::getMuscleGroup)
                .thenComparing(ExerciseCatalog::getName));
    }
}
