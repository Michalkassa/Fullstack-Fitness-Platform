package com.lockedin.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;

@JpaModelTest
class ExerciseCatalogTest {

    @Autowired
    TestEntityManager em;

    private List<ExerciseCatalog> all() {
        return em.getEntityManager()
            .createQuery("select c from ExerciseCatalog c order by c.name", ExerciseCatalog.class)
            .getResultList();
    }

    @Test
    void readsTheSeededCatalog() {
        assertThat(all()).hasSize(34);
    }

    @Test
    void mapsEveryColumn() {
        ExerciseCatalog bench = all().stream()
            .filter(c -> c.getName().equals("Barbell Bench Press"))
            .findFirst()
            .orElseThrow();

        assertThat(bench.getId()).isNotNull();
        assertThat(bench.getMuscleGroup()).isEqualTo("Chest");
        assertThat(bench.getEquipment()).isEqualTo("Barbell");
        assertThat(bench.getInstructions()).startsWith("Lie on a flat bench");
        assertThat(bench.getCreatedAt()).isNotNull();
    }

    @Test
    void isReadOnly() {
        ExerciseCatalog bench = all().getFirst();
        String original = bench.getName();

        // No setters exist; changing the managed entity's state reflectively must not be written back.
        org.springframework.test.util.ReflectionTestUtils.setField(bench, "name", "Changed");
        em.flush();
        em.clear();

        assertThat(em.find(ExerciseCatalog.class, bench.getId()).getName()).isEqualTo(original);
    }
}
