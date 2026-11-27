package com.lockedin.api.dto;

import com.lockedin.models.Xp;

/** {@code xpAwarded = logsCreated × Xp.PER_SET + Xp.PER_WORKOUT}. */
public record WorkoutSessionResult(int logsCreated, int xpAwarded) {
}
