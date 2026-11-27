package com.lockedin.workout;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.lockedin.common.BaseEntity;
import com.lockedin.exercise.Exercise;

/** Join row linking an exercise to a workout. Manage it through {@link Workout#addExercise}. */
@Entity
@Table(name = "workout_exercises",
    uniqueConstraints = @UniqueConstraint(name = "uq_workout_exercise", columnNames = {"workout_id", "exercise_id"}))
public class WorkoutExercise extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Workout workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Exercise exercise;

    protected WorkoutExercise() {
    }

    WorkoutExercise(Workout workout, Exercise exercise) {
        this.workout = Objects.requireNonNull(workout, "workout");
        this.exercise = Objects.requireNonNull(exercise, "exercise");
    }

    public Workout getWorkout() {
        return workout;
    }

    public Exercise getExercise() {
        return exercise;
    }
}
