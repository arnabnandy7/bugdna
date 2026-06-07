package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BugDnaTest {

    @Test
    void nearbyLineNumbersProduceTheSameFingerprint() {
        Throwable line57 = failureAt("com.example.UserService", "getUser", 57);
        Throwable line59 = failureAt("com.example.UserService", "getUser", 59);

        Fingerprint first = BugDna.generate(line57);
        Fingerprint second = BugDna.generate(line59);

        assertEquals(first, second);
        assertEquals("java.lang.NullPointerException", first.getRootCause());
        assertEquals("UserService#getUser", first.getSignature());
        assertEquals("com.example.UserService#getUser", first.getQualifiedSignature());
        assertTrue(first.getId().matches("BUGDNA-[0-9A-F]{16}"));
    }

    @Test
    void exceptionMessagesDoNotChangeTheFingerprint() {
        Throwable missingName = failureAt(
                new NullPointerException("name was null"),
                "com.example.UserService",
                "getUser",
                57
        );
        Throwable missingEmail = failureAt(
                new NullPointerException("email was null"),
                "com.example.UserService",
                "getUser",
                57
        );

        assertEquals(BugDna.generate(missingName), BugDna.generate(missingEmail));
    }

    @Test
    void differentMethodsProduceDifferentFingerprints() {
        Throwable getUser = failureAt("com.example.UserService", "getUser", 57);
        Throwable saveUser = failureAt("com.example.UserService", "saveUser", 57);

        assertNotEquals(BugDna.generate(getUser), BugDna.generate(saveUser));
    }

    @Test
    void sameSimpleClassNameInDifferentPackagesProducesDifferentFingerprints() {
        Throwable sales = failureAt("com.example.sales.UserService", "getUser", 57);
        Throwable admin = failureAt("com.example.admin.UserService", "getUser", 57);

        assertNotEquals(BugDna.generate(sales), BugDna.generate(admin));
    }

    @Test
    void differentCallPathsInTheSameMethodProduceDifferentFingerprints() {
        Throwable apiCall = failureWithFrames(
                new NullPointerException(),
                frame("com.example.UserService", "getUser", 57),
                frame("com.example.UserController", "show", 20)
        );
        Throwable batchCall = failureWithFrames(
                new NullPointerException(),
                frame("com.example.UserService", "getUser", 57),
                frame("com.example.UserImporter", "importUsers", 80)
        );

        assertNotEquals(BugDna.generate(apiCall), BugDna.generate(batchCall));
    }

    @Test
    void usesTheDeepestCause() {
        Throwable root = failureAt(
                new NullPointerException("missing user"),
                "com.example.UserService",
                "getUser",
                57
        );
        Throwable wrapper = new IllegalStateException("request failed", root);
        wrapper.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.example.UserController", "show", "UserController.java", 20)
        });

        Fingerprint fingerprint = BugDna.generate(wrapper);

        assertEquals("java.lang.NullPointerException", fingerprint.getRootCause());
        assertEquals("UserService#getUser", fingerprint.getSignature());
        assertEquals(
                Arrays.asList(
                        "java.lang.IllegalStateException",
                        "java.lang.NullPointerException"
                ),
                fingerprint.getCauseChain()
        );
        assertTrue(fingerprint.getExplanation().contains("Cause chain:"));
    }

    @Test
    void wrapperChangesDoNotSplitTheSameRootFailure() {
        Throwable firstRoot = failureAt("com.example.UserService", "getUser", 57);
        Throwable secondRoot = failureAt("com.example.UserService", "getUser", 59);

        Throwable serviceWrapper = new IllegalStateException("service failed", firstRoot);
        Throwable requestWrapper = new RuntimeException("request failed", secondRoot);

        assertEquals(BugDna.generate(serviceWrapper), BugDna.generate(requestWrapper));
    }

    @Test
    void limitsFingerprintToFiveNormalizedFrames() {
        Throwable failure = failureWithFrames(
                new NullPointerException(),
                frame("example.A", "one", 1),
                frame("example.B", "two", 2),
                frame("example.C", "three", 3),
                frame("example.D", "four", 4),
                frame("example.E", "five", 5),
                frame("example.F", "six", 6)
        );

        Fingerprint fingerprint = BugDna.generate(failure);

        assertEquals(5, fingerprint.getFrames().size());
        assertEquals("example.A#one", fingerprint.getFrames().get(0));
        assertEquals("example.E#five", fingerprint.getFrames().get(4));
    }

    @Test
    void exposesSimplifiedFailureChainForLogs() {
        Throwable failure = failureWithFrames(
                new NullPointerException(),
                frame("com.example.Repository", "find", 10),
                frame("com.example.Service", "getUser", 20),
                frame("com.example.Controller", "show", 30)
        );

        Fingerprint fingerprint = BugDna.generate(failure);

        assertEquals(
                Arrays.asList("Controller", "Service", "Repository"),
                fingerprint.getFailureChain()
        );
    }

    @Test
    void explainsFingerprintAsLogFriendlyBlock() {
        Throwable failure = failureWithFrames(
                new NullPointerException(),
                frame("com.example.Repository", "find", 10),
                frame("com.example.Service", "getUser", 20),
                frame("com.example.Controller", "show", 30)
        );

        Fingerprint fingerprint = BugDna.generate(failure);

        assertEquals(
                fingerprint.getId()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Root Cause:"
                        + System.lineSeparator()
                        + "NullPointerException"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Origin:"
                        + System.lineSeparator()
                        + "Repository#find"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Failure Chain:"
                        + System.lineSeparator()
                        + "Controller -> Service -> Repository",
                fingerprint.explain()
        );
    }

    @Test
    void returnsUnknownPriorityWithoutImpactContext() {
        Fingerprint fingerprint = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 57)
        );

        assertEquals(FailurePriority.UNKNOWN, fingerprint.getPriority());
        assertTrue(fingerprint.getExplanation().contains("Priority is unknown"));
    }

    @Test
    void prioritizesUsingOperationalImpact() {
        Throwable failure = failureAt("com.example.UserService", "getUser", 57);

        assertEquals(
                FailurePriority.LOW,
                BugDna.generate(failure, FailureContext.of(1, 0, false)).getPriority()
        );
        assertEquals(
                FailurePriority.MEDIUM,
                BugDna.generate(failure, FailureContext.of(10, 1, false)).getPriority()
        );
        assertEquals(
                FailurePriority.HIGH,
                BugDna.generate(failure, FailureContext.of(100, 10, false)).getPriority()
        );
        assertEquals(
                FailurePriority.CRITICAL,
                BugDna.generate(failure, FailureContext.of(1, 0, true)).getPriority()
        );
    }

    @Test
    void priorityDoesNotChangeFailureIdentity() {
        Throwable first = failureAt("com.example.UserService", "getUser", 57);
        Throwable second = failureAt("com.example.UserService", "getUser", 59);

        Fingerprint low = BugDna.generate(first, FailureContext.of(1, 0, false));
        Fingerprint critical = BugDna.generate(second, FailureContext.of(1000, 100, true));

        assertEquals(low.getId(), critical.getId());
        assertEquals(low, critical);
        assertNotEquals(low.getPriority(), critical.getPriority());
    }

    @Test
    void fingerprintCollectionsAreImmutable() {
        Fingerprint fingerprint = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 57)
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> fingerprint.getFrames().add("example.Other#method")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> fingerprint.getCauseChain().clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> fingerprint.getFailureChain().clear()
        );
    }

    @Test
    void rejectsInvalidImpactContext() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FailureContext.of(-1, 0, false)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> FailureContext.of(0, -1, false)
        );
    }

    @Test
    void rejectsNullFailures() {
        assertThrows(NullPointerException.class, () -> BugDna.generate(null));
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

    private static Throwable failureWithFrames(
            Throwable failure,
            StackTraceElement... frames
    ) {
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
