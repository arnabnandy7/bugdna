package io.github.bugdna;

import org.junit.jupiter.api.Test;

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
        assertTrue(first.getId().matches("BUGDNA-[0-9A-F]{6}"));
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
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(className, methodName, className + ".java", lineNumber)
        });
        return failure;
    }
}
