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
    void reportsCommonApplicationLayerChanges() {
        verifyLayerChange(
                "Controller Layer Changed",
                "com.example.UserController",
                "com.example.AccountController",
                "show"
        );
        verifyLayerChange(
                "Gateway Layer Changed",
                "com.example.PaymentGateway",
                "com.example.InvoiceGateway",
                "charge"
        );
        verifyLayerChange(
                "Client Layer Changed",
                "com.example.UserClient",
                "com.example.AccountClient",
                "load"
        );
        verifyLayerChange(
                "Handler Layer Changed",
                "com.example.UserHandler",
                "com.example.AccountHandler",
                "handle"
        );
        verifyLayerChange(
                "Validator Layer Changed",
                "com.example.UserValidator",
                "com.example.AccountValidator",
                "check"
        );
        verifyLayerChange(
                "Configuration Layer Changed",
                "com.example.DatabaseConfig",
                "com.example.SecurityConfiguration",
                "load"
        );
        verifyLayerChange(
                "Mapper Layer Changed",
                "com.example.UserMapper",
                "com.example.AccountMapper",
                "map"
        );
        verifyLayerChange(
                "Codec Layer Changed",
                "com.example.JsonCodec",
                "com.example.XmlCodec",
                "decode"
        );
        verifyLayerChange(
                "Application Layer Changed",
                "com.example.UserJob",
                "com.example.AccountJob",
                "run"
        );
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
    void reportsCallPathChangesWhenOriginAndRootCauseAreTheSame() {
        FingerprintDiff diff = BugDiff.compare(
                BugDna.generate(
                        failureWithFrames(
                                frame("com.example.UserService", "get", 10),
                                frame("com.example.UserController", "show", 20)
                        )
                ),
                BugDna.generate(
                        failureWithFrames(
                                frame("com.example.UserService", "get", 10),
                                frame("com.example.UserJob", "run", 20)
                        )
                )
        );

        assertEquals("Call Path Changed", diff.getSummary());
        assertEquals("UserService#get", diff.getOldValue());
        assertEquals("UserService#get", diff.getNewValue());
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

    private static void verifyLayerChange(
            String expectedSummary,
            String oldClassName,
            String newClassName,
            String methodName
    ) {
        assertEquals(
                expectedSummary,
                BugDiff.compare(
                        failureAt(oldClassName, methodName, 10),
                        failureAt(newClassName, methodName, 12)
                ).getSummary()
        );
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

    private static Throwable failureWithFrames(StackTraceElement... frames) {
        NullPointerException failure = new NullPointerException();
        failure.setStackTrace(frames);
        return failure;
    }

    private static StackTraceElement frame(String className, String methodName, int lineNumber) {
        return new StackTraceElement(className, methodName, className + ".java", lineNumber);
    }
}
