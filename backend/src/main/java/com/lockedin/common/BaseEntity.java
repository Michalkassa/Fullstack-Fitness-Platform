package com.lockedin.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/**
 * UUID primary key plus a {@code created_at} timestamp. The timestamp is filled in on
 * insert unless it was set explicitly (e.g. every set in a workout session shares one
 * timestamp), and is truncated to microseconds to match PostgreSQL's precision.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
