package com.lockedin.user;

/** Level progress calculated from total XP; see {@link Xp#levelProgress(int)}. */
public record UserStatsResponse(int xp, int level, int current, int needed, int percent) {
}
