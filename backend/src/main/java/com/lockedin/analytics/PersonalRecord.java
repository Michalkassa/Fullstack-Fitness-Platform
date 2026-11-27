package com.lockedin.analytics;

import java.time.LocalDate;
import java.util.UUID;

/** {@code achievedAt} is the day of the best one-rep max (the first one if two sets tie). */
public record PersonalRecord(
    UUID exerciseId, String exerciseName, double bestWeight, int bestOneRepMax, double bestVolume, LocalDate achievedAt) {
}
