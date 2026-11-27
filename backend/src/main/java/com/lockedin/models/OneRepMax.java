package com.lockedin.models;

/**
 * Estimated one-rep max using the Brzycki formula:
 * {@code floor(weight / (1.0278 - 0.0278 × reps))}.
 *
 * <p>The formula's denominator reaches zero just above 36 reps, so sets are capped at
 * {@link #MAX_REPS}. Request validation applies the same cap.
 */
public final class OneRepMax {

    public static final int MAX_REPS = 36;

    private OneRepMax() {
    }

    public static int estimate(double weight, int reps) {
        if (!Double.isFinite(weight) || weight < 0) {
            throw new IllegalArgumentException("weight must be a finite number >= 0: " + weight);
        }
        if (reps < 1 || reps > MAX_REPS) {
            throw new IllegalArgumentException("reps must be between 1 and " + MAX_REPS + ": " + reps);
        }
        return (int) Math.floor(weight / (1.0278 - 0.0278 * reps));
    }
}
