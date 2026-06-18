package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.sql.SQLTimeoutException;

import static io.github.bugdna.BugDnaAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BugDnaAssertionsTest {

    @Test
    void verifiesFingerprintWithFluentAssertions() {
        Fingerprint fingerprint = BugDna.generate(failureAt(
                new SQLTimeoutException("query timed out"),
                "com.example.UserRepository",
                "find",
                10
        ));

        assertSame(
                fingerprint,
                assertThat(fingerprint)
                        .hasCategory(FailureCategory.DATABASE)
                        .hasFamily(FailureFamily.DATABASE_CONNECTIVITY)
                        .hasRootCause(SQLTimeoutException.class)
                        .hasRootCause("java.sql.SQLTimeoutException")
                        .hasId(fingerprint.getId())
                        .hasSignature("UserRepository#find")
                        .hasQualifiedSignature("com.example.UserRepository#find")
                        .hasStabilityScore(90)
                        .actual()
        );
    }

    @Test
    void failsWithHelpfulMessageWhenValuesDiffer() {
        Fingerprint fingerprint = BugDna.generate(failureAt(
                new SQLTimeoutException("query timed out"),
                "com.example.UserRepository",
                "find",
                10
        ));
        BugDnaAssertions.FingerprintAssert assertion = assertThat(fingerprint);

        AssertionError error = assertThrows(
                AssertionError.class,
                () -> assertion.hasCategory(FailureCategory.NETWORK)
        );

        assertEquals(
                "Expected fingerprint category to be <NETWORK> but was <DATABASE>.",
                error.getMessage()
        );
    }

    @Test
    void rejectsNullFingerprintAsAssertionFailure() {
        AssertionError error = assertThrows(
                AssertionError.class,
                () -> assertThat(null)
        );

        assertEquals("Expected fingerprint to be non-null.", error.getMessage());
    }

    @Test
    void rejectsNullExpectedValues() {
        Fingerprint fingerprint = BugDna.generate(failureAt(
                new IllegalArgumentException("invalid"),
                "com.example.UserValidator",
                "validate",
                10
        ));
        BugDnaAssertions.FingerprintAssert assertion = assertThat(fingerprint);
        Class<? extends Throwable> nullRootCauseClass = null;
        String nullValue = null;

        assertThrows(NullPointerException.class, () -> assertion.hasCategory(null));
        assertThrows(NullPointerException.class, () -> assertion.hasFamily(null));
        assertThrows(NullPointerException.class, () -> assertion.hasRootCause(nullRootCauseClass));
        assertThrows(NullPointerException.class, () -> assertion.hasRootCause(nullValue));
        assertThrows(NullPointerException.class, () -> assertion.hasId(nullValue));
        assertThrows(NullPointerException.class, () -> assertion.hasSignature(nullValue));
        assertThrows(NullPointerException.class, () -> assertion.hasQualifiedSignature(nullValue));
    }

    @Test
    void reportsFailuresForEveryAssertionField() {
        Fingerprint fingerprint = BugDna.generate(failureAt(
                new IllegalArgumentException("invalid"),
                "com.example.UserValidator",
                "validate",
                10
        ));

        verifyFails(() -> assertThat(fingerprint).hasFamily(FailureFamily.DATABASE_OPERATION), "family");
        verifyFails(() -> assertThat(fingerprint).hasRootCause(IllegalStateException.class), "root cause");
        verifyFails(() -> assertThat(fingerprint).hasRootCause("example.OtherException"), "root cause");
        verifyFails(() -> assertThat(fingerprint).hasId("BUGDNA-OTHER"), "id");
        verifyFails(() -> assertThat(fingerprint).hasSignature("Other#method"), "signature");
        verifyFails(
                () -> assertThat(fingerprint).hasQualifiedSignature("example.Other#method"),
                "qualified signature"
        );
        verifyFails(() -> assertThat(fingerprint).hasStabilityScore(98), "stability score");
    }

    private static void verifyFails(Runnable assertion, String field) {
        AssertionError error = assertThrows(AssertionError.class, assertion::run);

        assertTrue(error.getMessage().contains("Expected fingerprint " + field));
    }

    private static Throwable failureAt(
            Throwable failure,
            String className,
            String methodName,
            int lineNumber
    ) {
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(className, methodName, className + ".java", lineNumber)
        });
        return failure;
    }
}
