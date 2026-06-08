package io.github.bugdna;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BugSimilarityTest {

    @Test
    void sameFingerprintIsOneHundredPercentSimilar() {
        Fingerprint first = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 57)
        );
        Fingerprint second = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 59)
        );

        Similarity similarity = BugSimilarity.compare(first, second);

        assertEquals(100, similarity.getPercentage());
        assertTrue(similarity.isLikelyRelated());
        assertTrue(similarity.getExplanation().contains("same id"));
    }

    @Test
    void similarMethodsInSameClassAreLikelyRelated() {
        Fingerprint getUser = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 57)
        );
        Fingerprint getUserById = BugDna.generate(
                failureAt("com.example.UserService", "getUserById", 88)
        );

        Similarity similarity = BugSimilarity.compare(getUser, getUserById);

        assertNotEquals(getUser, getUserById);
        assertEquals(92, similarity.getPercentage());
        assertTrue(similarity.isLikelyRelated());
    }

    @Test
    void unrelatedFailuresScoreLow() {
        Fingerprint userFailure = BugDna.generate(
                failureAt(new NullPointerException(), "com.example.UserService", "getUser", 57)
        );
        Fingerprint paymentFailure = BugDna.generate(
                failureAt(new IllegalArgumentException(), "com.example.PaymentGateway", "charge", 40)
        );

        Similarity similarity = BugSimilarity.compare(userFailure, paymentFailure);

        assertEquals(0, similarity.getPercentage());
        assertFalse(similarity.isLikelyRelated());
    }

    @Test
    void sharedCallPathRaisesSimilarityButDoesNotRequireSameMethod() {
        Fingerprint getUser = BugDna.generate(
                failureWithFrames(
                        new NullPointerException(),
                        frame("com.example.UserService", "getUser", 57),
                        frame("com.example.UserRepository", "find", 20)
                )
        );
        Fingerprint loadUser = BugDna.generate(
                failureWithFrames(
                        new NullPointerException(),
                        frame("com.example.UserService", "loadUser", 62),
                        frame("com.example.UserRepository", "find", 20)
                )
        );

        Similarity similarity = BugSimilarity.compare(getUser, loadUser);

        assertTrue(similarity.getPercentage() >= 80);
        assertTrue(similarity.isLikelyRelated());
        assertTrue(similarity.getExplanation().contains("frames="));
    }

    @Test
    void similarityResultHasUsefulStringRepresentation() {
        Fingerprint first = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 57)
        );
        Fingerprint second = BugDna.generate(
                failureAt("com.example.UserService", "getUserById", 88)
        );

        String value = BugSimilarity.compare(first, second).toString();

        assertTrue(value.contains("Similarity{"));
        assertTrue(value.contains("percentage=92"));
        assertTrue(value.contains("likelyRelated=true"));
    }

    @Test
    void rejectsNullFingerprints() {
        Fingerprint fingerprint = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 57)
        );

        assertThrows(NullPointerException.class, () -> BugSimilarity.compare(null, fingerprint));
        assertThrows(NullPointerException.class, () -> BugSimilarity.compare(fingerprint, null));
    }

    @Test
    void rejectsInvalidSimilarityPercentages() {
        assertThrows(IllegalArgumentException.class, () -> new Similarity(-1, "too low"));
        assertThrows(IllegalArgumentException.class, () -> new Similarity(101, "too high"));
    }

    private static Throwable failureAt(String className, String methodName, int lineNumber) {
        return failureAt(new NullPointerException(), className, methodName, lineNumber);
    }

    private static Throwable failureAt(
            Throwable failure,
            String className,
            String methodName,
            int lineNumber
    ) {
        failure.setStackTrace(new StackTraceElement[] {frame(className, methodName, lineNumber)});
        return failure;
    }

    private static Throwable failureWithFrames(Throwable failure, StackTraceElement... frames) {
        failure.setStackTrace(frames);
        return failure;
    }

    private static StackTraceElement frame(
            String className,
            String methodName,
            int lineNumber
    ) {
        return new StackTraceElement(className, methodName, className + ".java", lineNumber);
    }
}
