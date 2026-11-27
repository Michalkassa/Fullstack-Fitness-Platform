package com.lockedin.models;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** One logged set of an exercise. */
@Entity
@Table(name = "logs")
public class Log extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Exercise exercise;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private int reps;

    @Column(name = "one_rep_max", nullable = false)
    private int oneRepMax;

    protected Log() {
    }

    public Log(User user, Exercise exercise, double weight, int reps, int oneRepMax) {
        this.user = Objects.requireNonNull(user, "user");
        this.exercise = Objects.requireNonNull(exercise, "exercise");
        this.weight = weight;
        this.reps = reps;
        this.oneRepMax = oneRepMax;
    }

    public User getUser() {
        return user;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public double getWeight() {
        return weight;
    }

    public int getReps() {
        return reps;
    }

    public int getOneRepMax() {
        return oneRepMax;
    }
}
