package com.lockedin.workout;

/** {@code xpAwarded = logsCreated × Xp.PER_SET + Xp.PER_WORKOUT}. */
public record WorkoutSessionResult(int logsCreated, int xpAwarded) {
}
