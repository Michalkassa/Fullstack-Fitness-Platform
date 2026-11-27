package com.lockedin.nutrition;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.lockedin.common.BaseEntity;
import com.lockedin.user.User;

@Entity
@Table(name = "nutrition_entries")
public class NutritionEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
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

    protected NutritionEntry() {
    }

    public NutritionEntry(User user, int calories) {
        this(user, calories, 0, 0, 0);
    }

    public NutritionEntry(User user, int calories, double protein, double carbs, double fat) {
        this.user = Objects.requireNonNull(user, "user");
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
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
}
