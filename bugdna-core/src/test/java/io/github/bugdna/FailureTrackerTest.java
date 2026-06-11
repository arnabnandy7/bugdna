package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureTrackerTest {

    @Test
    void aggregatesFailuresByFingerprintAndReportsMostFrequentFirst() {
        FailureTracker tracker = new FailureTracker();
        Throwable frequent = failureAt("com.example.UserService", "load", 10);
        Throwable occasional = failureAt("com.example.OrderService", "save", 20);

        Fingerprint frequentFingerprint = tracker.capture(frequent);
        tracker.capture(frequent);
        Fingerprint occasionalFingerprint = tracker.capture(occasional);

        List<FailureAggregate> failures = tracker.failures();
        assertEquals(3, tracker.getTotalOccurrences());
        assertEquals(2, tracker.getUniqueFailures());
        assertEquals(frequentFingerprint.getId(), failures.get(0).getId());
        assertEquals(2, failures.get(0).getOccurrences());
        assertEquals(occasionalFingerprint.getId(), failures.get(1).getId());
        assertEquals(
                frequentFingerprint.getId()
                        + System.lineSeparator()
                        + "Occurrences: 2"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + occasionalFingerprint.getId()
                        + System.lineSeparator()
                        + "Occurrences: 1",
                tracker.report()
        );
        assertThrows(UnsupportedOperationException.class, failures::clear);
    }

    @Test
    void safelyCountsConcurrentCaptures() throws InterruptedException {
        FailureTracker tracker = new FailureTracker();
        Fingerprint fingerprint = BugDna.generate(
                failureAt("com.example.UserService", "load", 10)
        );
        ExecutorService executor = Executors.newFixedThreadPool(8);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> tracker.capture(fingerprint));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1000, tracker.getTotalOccurrences());
        assertEquals(1, tracker.getUniqueFailures());
        assertEquals(1000, tracker.failures().get(0).getOccurrences());

        tracker.clear();
        assertEquals(0, tracker.getTotalOccurrences());
        assertTrue(tracker.failures().isEmpty());
    }

    @Test
    void rejectsNullCaptures() {
        FailureTracker tracker = new FailureTracker();

        assertThrows(NullPointerException.class, () -> tracker.capture((Throwable) null));
        assertThrows(NullPointerException.class, () -> tracker.capture((Fingerprint) null));
    }

    private static Throwable failureAt(String className, String methodName, int lineNumber) {
        NullPointerException failure = new NullPointerException();
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(className, methodName, className + ".java", lineNumber)
        });
        return failure;
    }
}
