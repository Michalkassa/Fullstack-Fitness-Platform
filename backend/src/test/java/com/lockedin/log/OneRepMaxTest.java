package com.lockedin.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class OneRepMaxTest {

    @ParameterizedTest(name = "{0} x {1} -> {2}")
    @CsvSource({
        "100, 1, 100",
        "0, 5, 0",
        "100, 5, 112",
        "100, 10, 133",
    })
    void matchesTheBrzyckiExamplesFromTheSpec(double weight, int reps, int expected) {
        assertThat(OneRepMax.estimate(weight, reps)).isEqualTo(expected);
    }

    @Test
    void aSingleRepIsTheWeightItself() {
        assertThat(OneRepMax.estimate(142.5, 1)).isEqualTo(142);
    }

    @Test
    void moreRepsAtTheSameWeightGiveAHigherEstimate() {
        int previous = OneRepMax.estimate(100, 1);
        for (int reps = 2; reps <= OneRepMax.MAX_REPS; reps++) {
            int current = OneRepMax.estimate(100, reps);
            assertThat(current).as("reps=%d", reps).isGreaterThanOrEqualTo(previous);
            previous = current;
        }
        assertThat(OneRepMax.estimate(100, 10)).isGreaterThan(OneRepMax.estimate(100, 5));
    }

    @Test
    void allowsUpToThirtySixReps() {
        assertThat(OneRepMax.MAX_REPS).isEqualTo(36);
        assertThat(OneRepMax.estimate(100, 36)).isPositive();
    }

    @ParameterizedTest
    @ValueSource(ints = {37, 38, 100})
    void rejectsMoreThanThirtySixReps(int reps) {
        assertThatThrownBy(() -> OneRepMax.estimate(100, reps)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsRepsBelowOne(int reps) {
        assertThatThrownBy(() -> OneRepMax.estimate(100, reps)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeWeight() {
        assertThatThrownBy(() -> OneRepMax.estimate(-1, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonFiniteWeight() {
        assertThatThrownBy(() -> OneRepMax.estimate(Double.NaN, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OneRepMax.estimate(Double.POSITIVE_INFINITY, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
