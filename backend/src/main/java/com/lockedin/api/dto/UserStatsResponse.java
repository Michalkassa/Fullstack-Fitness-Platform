package com.lockedin.api.dto;

import com.lockedin.models.Xp;

/** Level progress calculated from total XP; see {@link Xp#levelProgress(int)}. */
public record UserStatsResponse(int xp, int level, int current, int needed, int percent) {
}
