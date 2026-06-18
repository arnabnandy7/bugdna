package io.github.bugdna;

import java.util.Objects;

/**
 * Fluent assertions for BugDNA fingerprints in automated tests.
 */
public final class BugDnaAssertions {

    private static final String EXPECTED_MUST_NOT_BE_NULL = "expected must not be null";

    private BugDnaAssertions() {
    }

    /**
     * Starts a fluent assertion chain for a fingerprint.
     *
     * @param fingerprint fingerprint under test
     * @return fingerprint assertion
     * @throws AssertionError when {@code fingerprint} is {@code null}
     */
    public static FingerprintAssert assertThat(Fingerprint fingerprint) {
        return new FingerprintAssert(fingerprint);
    }

    /**
     * Fluent assertions for {@link Fingerprint}.
     */
    public static final class FingerprintAssert {

        private final Fingerprint fingerprint;

        private FingerprintAssert(Fingerprint fingerprint) {
            if (fingerprint == null) {
                throw new AssertionError("Expected fingerprint to be non-null.");
            }
            this.fingerprint = fingerprint;
        }

        /**
         * Verifies the fingerprint category.
         *
         * @param expected expected category
         * @return this assertion
         */
        public FingerprintAssert hasCategory(FailureCategory expected) {
            FailureCategory requiredExpected = Objects.requireNonNull(
                    expected,
                    EXPECTED_MUST_NOT_BE_NULL
            );
            if (fingerprint.getCategory() != requiredExpected) {
                fail("category", requiredExpected, fingerprint.getCategory());
            }
            return this;
        }

        /**
         * Verifies the fingerprint family.
         *
         * @param expected expected family
         * @return this assertion
         */
        public FingerprintAssert hasFamily(FailureFamily expected) {
            FailureFamily requiredExpected = Objects.requireNonNull(
                    expected,
                    EXPECTED_MUST_NOT_BE_NULL
            );
            if (fingerprint.getFamily() != requiredExpected) {
                fail("family", requiredExpected, fingerprint.getFamily());
            }
            return this;
        }

        /**
         * Verifies the root-cause exception class.
         *
         * @param expected expected root-cause class
         * @return this assertion
         */
        public FingerprintAssert hasRootCause(Class<? extends Throwable> expected) {
            Class<? extends Throwable> requiredExpected = Objects.requireNonNull(
                    expected,
                    EXPECTED_MUST_NOT_BE_NULL
            );
            return hasRootCause(requiredExpected.getName());
        }

        /**
         * Verifies the root-cause class name.
         *
         * @param expected expected root-cause class name
         * @return this assertion
         */
        public FingerprintAssert hasRootCause(String expected) {
            String requiredExpected = Objects.requireNonNull(
                    expected,
                    EXPECTED_MUST_NOT_BE_NULL
            );
            if (!fingerprint.getRootCause().equals(requiredExpected)) {
                fail("root cause", requiredExpected, fingerprint.getRootCause());
            }
            return this;
        }

        /**
         * Verifies the fingerprint ID.
         *
         * @param expected expected fingerprint ID
         * @return this assertion
         */
        public FingerprintAssert hasId(String expected) {
            String requiredExpected = Objects.requireNonNull(
                    expected,
                    EXPECTED_MUST_NOT_BE_NULL
            );
            if (!fingerprint.getId().equals(requiredExpected)) {
                fail("id", requiredExpected, fingerprint.getId());
            }
            return this;
        }

        /**
         * Verifies the simple origin signature.
         *
         * @param expected expected signature
         * @return this assertion
         */
        public FingerprintAssert hasSignature(String expected) {
            String requiredExpected = Objects.requireNonNull(
                    expected,
                    EXPECTED_MUST_NOT_BE_NULL
            );
            if (!fingerprint.getSignature().equals(requiredExpected)) {
                fail("signature", requiredExpected, fingerprint.getSignature());
            }
            return this;
        }

        /**
         * Verifies the qualified origin signature.
         *
         * @param expected expected qualified signature
         * @return this assertion
         */
        public FingerprintAssert hasQualifiedSignature(String expected) {
            String requiredExpected = Objects.requireNonNull(
                    expected,
                    EXPECTED_MUST_NOT_BE_NULL
            );
            if (!fingerprint.getQualifiedSignature().equals(requiredExpected)) {
                fail(
                        "qualified signature",
                        requiredExpected,
                        fingerprint.getQualifiedSignature()
                );
            }
            return this;
        }

        /**
         * Verifies the stability score.
         *
         * @param expected expected stability score
         * @return this assertion
         */
        public FingerprintAssert hasStabilityScore(int expected) {
            if (fingerprint.getStabilityScore() != expected) {
                fail("stability score", Integer.valueOf(expected), Integer.valueOf(
                        fingerprint.getStabilityScore()
                ));
            }
            return this;
        }

        /**
         * Returns the asserted fingerprint for custom checks.
         *
         * @return asserted fingerprint
         */
        public Fingerprint actual() {
            return fingerprint;
        }

        private static void fail(String field, Object expected, Object actual) {
            throw new AssertionError(
                    "Expected fingerprint " + field + " to be <" + expected
                            + "> but was <" + actual + ">."
            );
        }
    }
}
