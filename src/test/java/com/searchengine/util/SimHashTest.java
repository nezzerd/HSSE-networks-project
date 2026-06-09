package com.searchengine.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimHashTest {

    @Test
    void identicalText_hasZeroDistance() {
        String text = "the quick brown fox jumps over the lazy dog near the river bank";
        long a = SimHash.compute(text);
        long b = SimHash.compute(text);

        assertThat(SimHash.hammingDistance(a, b)).isZero();
    }

    @Test
    void oneChangedWord_hasSmallDistance() {
        String original = "the quick brown fox jumps over the lazy dog near the river bank today";
        String changed = "the quick brown fox jumps over the lazy cat near the river bank today";

        long a = SimHash.compute(original);
        long b = SimHash.compute(changed);
        int distance = SimHash.hammingDistance(a, b);

        assertThat(distance).isGreaterThan(0);
        assertThat(distance).isLessThan(16);
    }

    @Test
    void completelyDifferentText_hasLargeDistance() {
        long a = SimHash.compute("the quick brown fox jumps over the lazy dog near the river");
        long b = SimHash.compute("spring boot lucene search engine indexing documents and queries");

        int distance = SimHash.hammingDistance(a, b);

        assertThat(distance).isGreaterThan(15);
    }

    @Test
    void blankText_returnsZero() {
        assertThat(SimHash.compute("")).isZero();
        assertThat(SimHash.compute(null)).isZero();
    }
}
