package com.lockedin.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class XpTest {

    @Test
    void awardsAreFromTheSpec() {
        assertThat(Xp.PER_SET).isEqualTo(10);
        assertThat(Xp.PER_WORKOUT).isEqualTo(25);
    }

    @ParameterizedTest(name = "level {0} needs {1} xp")
    @CsvSource({
        "1, 0",
        "2, 100",
        "3, 300",
        "4, 600",
        "10, 4500",
    })
    void xpForLevel(int level, int expected) {
        assertThat(Xp.xpForLevel(level)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} xp -> level {1}")
    @CsvSource({
        "0, 1",
        "99, 1",
        "100, 2",
        "299, 2",
        "300, 3",
        "599, 3",
        "600, 4",
        "4499, 9",
        "4500, 10",
    })
    void levelFromXp(int xp, int expected) {
        assertThat(Xp.levelFromXp(xp)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -500, Integer.MIN_VALUE})
    void negativeXpIsLevelOne(int xp) {
        assertThat(Xp.levelFromXp(xp)).isEqualTo(1);
    }

    @Test
    void everyLevelStartsExactlyAtItsThreshold() {
        for (int level = 1; level <= 1_000; level++) {
            int threshold = Xp.xpForLevel(level);
            assertThat(Xp.levelFromXp(threshold)).as("at %d xp", threshold).isEqualTo(level);
            if (level > 1) {
                assertThat(Xp.levelFromXp(threshold - 1)).as("at %d xp", threshold - 1).isEqualTo(level - 1);
            }
        }
    }

    @Test
    void handlesTheLargestPossibleXp() {
        int level = Xp.levelFromXp(Integer.MAX_VALUE);

        assertThat((long) Xp.xpForLevel(level)).isLessThanOrEqualTo(Integer.MAX_VALUE);
        assertThat(Xp.levelProgress(Integer.MAX_VALUE).level()).isEqualTo(level);
    }

    @ParameterizedTest(name = "{0} xp -> level {1}, {2}/{3} ({4}%)")
    @CsvSource({
        "0, 1, 0, 100, 0",
        "99, 1, 99, 100, 99",
        "100, 2, 0, 200, 0",
        "150, 2, 50, 200, 25",
        "299, 2, 199, 200, 100",
        "300, 3, 0, 300, 0",
        "600, 4, 0, 400, 0",
    })
    void levelProgressMatchesTheSpec(int xp, int level, int current, int needed, int percent) {
        assertThat(Xp.levelProgress(xp)).isEqualTo(new Xp.LevelProgress(level, current, needed, percent));
    }

    @Test
    void negativeXpHasNoProgress() {
        assertThat(Xp.levelProgress(-50)).isEqualTo(new Xp.LevelProgress(1, 0, 100, 0));
    }
}
