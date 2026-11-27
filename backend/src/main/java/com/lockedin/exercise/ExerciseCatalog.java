package com.lockedin.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import com.lockedin.common.BaseEntity;

/** Global, read-only exercise library seeded by Flyway. */
@Entity
@Immutable
@Table(name = "exercise_catalog")
public class ExerciseCatalog extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "muscle_group", nullable = false, length = 64)
    private String muscleGroup;

    @Column(nullable = false, length = 64)
    private String equipment;

    @Column(nullable = false, columnDefinition = "text")
    private String instructions;

    protected ExerciseCatalog() {
    }

    public String getName() {
        return name;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }

    public String getEquipment() {
        return equipment;
    }

    public String getInstructions() {
        return instructions;
    }
}
