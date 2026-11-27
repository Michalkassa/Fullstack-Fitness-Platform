package com.lockedin.models;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** A user's daily nutrition target. Each user has at most one. */
@Entity
@Table(name = "nutrition_goals")
public class NutritionGoal extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    private int calories;

    @Column(nullable = false)
    private double protein;

    @Column(nullable = false)
    private double carbs;

    @Column(nullable = false)
    private double fat;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NutritionGoal() {
    }

    public NutritionGoal(User user, int calories) {
        this(user, calories, 0, 0, 0);
    }

    public NutritionGoal(User user, int calories, double protein, double carbs, double fat) {
        this.user = Objects.requireNonNull(user, "user");
        update(calories, protein, carbs, fat);
    }

    public void update(int calories, double protein, double carbs, double fat) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        updatedAt = getCreatedAt();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = now();
    }

    public User getUser() {
        return user;
    }

    public int getCalories() {
        return calories;
    }

    public double getProtein() {
        return protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getFat() {
        return fat;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
