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

@Entity
@Table(name = "body_weights")
public class BodyWeight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    private double weight;

    protected BodyWeight() {
    }

    public BodyWeight(User user, double weight) {
        this.user = Objects.requireNonNull(user, "user");
        this.weight = weight;
    }

    public User getUser() {
        return user;
    }

    public double getWeight() {
        return weight;
    }
}
