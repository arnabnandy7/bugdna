package io.github.bugdna;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BugDiffTest {

    @Test
    void reportsRepositoryLayerChanges() {
        Throwable oldException = failureAt("com.example.UserRepository", "find", 10);
        Throwable newException = failureAt("com.example.CustomerRepository", "find", 12);

        FingerprintDiff diff = BugDiff.compare(oldException, newException);

        assertEquals("Repository Layer Changed", diff.getSummary());
        assertEquals("UserRepository", diff.getOldValue());
        assertEquals("CustomerRepository", diff.getNewValue());
        assertEquals(
                "Repository Layer Changed"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Old:"
                        + System.lineSeparator()
                        + "UserRepository"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "New:"
                        + System.lineSeparator()
                        + "CustomerRepository",
                diff.explain()
        );
    }

    @Test
    void reportsLayerChanges() {
        FingerprintDiff diff = BugDiff.compare(
                failureAt("com.example.UserService", "get", 10),
                failureAt("com.example.UserRepository", "find", 12)
        );

        assertEquals("Layer Changed", diff.getSummary());
        assertEquals("Service", diff.getOldValue());
        assertEquals("Repository", diff.getNewValue());
    }

    @Test
    void reportsMethodChangesInSameClass() {
        FingerprintDiff diff = BugDiff.compare(
                failureAt("com.example.UserService", "getUser", 10),
                failureAt("com.example.UserService", "loadUser", 12)
        );

        assertEquals("Method Changed", diff.getSummary());
        assertEquals("getUser", diff.getOldValue());
        assertEquals("loadUser", diff.getNewValue());
    }

    @Test
    void reportsRootCauseChangesWhenOriginIsTheSame() {
        FingerprintDiff diff = BugDiff.compare(
                failureAt(new NullPointerException(), "com.example.UserService", "get", 10),
                failureAt(new IllegalStateException(), "com.example.UserService", "get", 12)
        );

        assertEquals("Root Cause Changed", diff.getSummary());
        assertEquals("NullPointerException", diff.getOldValue());
        assertEquals("IllegalStateException", diff.getNewValue());
    }

    @Test
    void reportsNoChangeForSameFingerprint() {
        Throwable oldException = failureAt("com.example.UserService", "get", 10);
        Throwable newException = failureAt("com.example.UserService", "get", 12);

        FingerprintDiff diff = BugDiff.compare(oldException, newException);

        assertEquals("No Fingerprint Change", diff.getSummary());
        assertTrue(diff.getOldValue().startsWith("BUGDNA-"));
        assertEquals(diff.getOldValue(), diff.getNewValue());
    }

    @Test
    void acceptsExistingFingerprints() {
        Fingerprint oldFingerprint = BugDna.generate(
                failureAt("com.example.UserRepository", "find", 10)
        );
        Fingerprint newFingerprint = BugDna.generate(
                failureAt("com.example.CustomerRepository", "find", 12)
        );

        FingerprintDiff diff = BugDiff.compare(oldFingerprint, newFingerprint);

        assertEquals("Repository Layer Changed", diff.getSummary());
    }

    @Test
    void diffResultHasUsefulStringRepresentation() {
        FingerprintDiff diff = BugDiff.compare(
                failureAt("com.example.UserService", "getUser", 10),
                failureAt("com.example.UserService", "loadUser", 12)
        );

        String value = diff.toString();

        assertTrue(value.contains("FingerprintDiff{"));
        assertTrue(value.contains("summary='Method Changed'"));
        assertTrue(value.contains("oldValue='getUser'"));
    }

    @Test
    void rejectsNullInputs() {
        Throwable exception = failureAt("com.example.UserService", "get", 10);
        Fingerprint fingerprint = BugDna.generate(exception);

        assertThrows(NullPointerException.class, () -> BugDiff.compare(null, exception));
        assertThrows(NullPointerException.class, () -> BugDiff.compare(exception, null));
        assertThrows(NullPointerException.class, () -> BugDiff.compare(null, fingerprint));
        assertThrows(NullPointerException.class, () -> BugDiff.compare(fingerprint, null));
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
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(className, methodName, className + ".java", lineNumber)
        });
        return failure;
    }
}
