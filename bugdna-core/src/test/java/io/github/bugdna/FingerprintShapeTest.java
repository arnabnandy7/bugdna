package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FingerprintShapeTest {

    @Test
    void methodSimilarityReturnsZeroWhenOnlyFirstMethodTokenizesBlank() {
        assertEquals(0.0d, FingerprintShape.methodSimilarity("   ", "load"));
    }

    @Test
    void overlapReturnsZeroWhenEitherSideIsEmpty() {
        assertEquals(
                0.0d,
                FingerprintShape.overlap(
                        Collections.emptyList(),
                        Collections.singletonList("java.lang.IllegalStateException")
                )
        );
        assertEquals(
                0.0d,
                FingerprintShape.overlap(
                        Collections.singletonList("java.lang.IllegalStateException"),
                        Collections.emptyList()
                )
        );
    }
}
