package com.lockedin.models;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String name;

    @Column(length = 1024)
    private String image;

    @Column(nullable = false)
    private int xp;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
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

    public void addXp(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("XP amount must not be negative: " + amount);
        }
        xp += amount;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getXp() {
        return xp;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
