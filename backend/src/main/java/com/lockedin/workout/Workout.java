package com.lockedin.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.lockedin.common.BaseEntity;
import com.lockedin.exercise.Exercise;
import com.lockedin.user.User;

/**
 * A named group of exercises. Public templates (the seeded programs) are owned by the
 * system user and copied by others.
 */
@Entity
@Table(name = "workouts")
public class Workout extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "is_template", nullable = false)
    private boolean isTemplate;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt")
    private List<WorkoutExercise> exercises = new ArrayList<>();

    protected Workout() {
    }

    public Workout(User user, String name, String description) {
        this.user = Objects.requireNonNull(user, "user");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
    }

    /** Adds the exercise unless it's already in this workout. Returns whether it was added. */
    public boolean addExercise(Exercise exercise) {
        Objects.requireNonNull(exercise, "exercise");
        if (contains(exercise)) {
            return false;
        }
        exercises.add(new WorkoutExercise(this, exercise));
        return true;
    }

    /** Removes the exercise from this workout. Returns whether it was there. */
    public boolean removeExercise(Exercise exercise) {
        return exercises.removeIf(we -> sameExercise(we.getExercise(), exercise));
    }

    private boolean contains(Exercise exercise) {
        return exercises.stream().anyMatch(we -> sameExercise(we.getExercise(), exercise));
    }

    private static boolean sameExercise(Exercise a, Exercise b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean template) {
        this.isTemplate = template;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<WorkoutExercise> getExercises() {
        return Collections.unmodifiableList(exercises);
    }
}
