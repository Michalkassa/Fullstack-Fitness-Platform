package com.lockedin.user;

/**
 * XP awards and the level curve. Reaching level n takes {@code 50 × (n − 1) × n} XP
 * (level 1 = 0, 2 = 100, 3 = 300, 4 = 600). Negative XP counts as level 1.
 */
public final class Xp {

    public static final int PER_SET = 10;
    public static final int PER_WORKOUT = 25;

    /** Progress through the current level. {@code percent} is rounded, so it can read 100 just before a level-up. */
    public record LevelProgress(int level, int current, int needed, int percent) {
    }

    private Xp() {
    }

    /** Total XP needed to reach {@code level}. */
    public static int xpForLevel(int level) {
        return Math.toIntExact(threshold(level));
    }

    /** The highest level whose threshold is at most {@code xp}. */
    public static int levelFromXp(int xp) {
        if (xp <= 0) {
            return 1;
        }
        // Solve 50n(n − 1) <= xp for n, then correct any floating-point drift.
        int level = (int) Math.floor((1 + Math.sqrt(1 + 0.08 * xp)) / 2);
        while (threshold(level) > xp) {
            level--;
        }
        while (threshold(level + 1) <= xp) {
            level++;
        }
        return level;
    }

    public static LevelProgress levelProgress(int xp) {
        int clamped = Math.max(xp, 0);
        int level = levelFromXp(clamped);
        long start = threshold(level);
        int current = (int) (clamped - start);
        int needed = (int) (threshold(level + 1) - start);
        int percent = (int) Math.round(current * 100.0 / needed);
        return new LevelProgress(level, current, needed, percent);
    }

    private static long threshold(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1: " + level);
        }
        return 50L * (level - 1) * level;
    }
}
