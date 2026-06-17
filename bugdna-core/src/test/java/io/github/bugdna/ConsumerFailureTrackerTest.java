package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumerFailureTrackerTest {

    @Test
    void groupsConsumerFailuresByTopicAndFingerprint() {
        ConsumerFailureTracker tracker = new ConsumerFailureTracker();
        Fingerprint paymentFailure = fingerprintAt("com.example.PaymentConsumer", "consume", 10);

        tracker.capture("payment-events", 0, 100, paymentFailure);
        tracker.capture("payment-events", 2, 302, paymentFailure);

        ConsumerFailureAggregate failure = tracker.failures().get(0);
        assertPaymentFailureAggregate(paymentFailure, failure);
        assertPaymentReport(paymentFailure, tracker.report());
    }

    private static void assertPaymentFailureAggregate(
            Fingerprint paymentFailure,
            ConsumerFailureAggregate failure
    ) {
        assertEquals(paymentFailure.getId(), failure.getId());
        assertEquals("payment-events", failure.getTopic());
        assertEquals(2, failure.getPartition());
        assertEquals(302, failure.getOffset());
        assertEquals(2, failure.getOccurrences());
    }

    private static void assertPaymentReport(Fingerprint paymentFailure, String report) {
        assertEquals(
                paymentFailure.getId()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Topic:"
                        + System.lineSeparator()
                        + "payment-events"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Occurrences:"
                        + System.lineSeparator()
                        + "2",
                report
        );
    }

    @Test
    void keepsTheSameFingerprintSeparateAcrossTopics() {
        ConsumerFailureTracker tracker = new ConsumerFailureTracker();
        Fingerprint fingerprint = fingerprintAt("com.example.EventConsumer", "consume", 10);

        tracker.capture("payment-events", 0, 1, fingerprint);
        tracker.capture("refund-events", 0, 2, fingerprint);

        List<ConsumerFailureAggregate> failures = tracker.failures();
        assertEquals(2, failures.size());
        assertEquals("payment-events", failures.get(0).getTopic());
        assertEquals("refund-events", failures.get(1).getTopic());
        assertThrows(UnsupportedOperationException.class, failures::clear);
    }

    @Test
    void capturesThrowablesValidatesMetadataAndClears() {
        ConsumerFailureTracker tracker = new ConsumerFailureTracker();
        Throwable failure = failureAt("com.example.PaymentConsumer", "consume", 10);

        Fingerprint fingerprint = tracker.capture("payment-events", 1, 203, failure);

        assertEquals(fingerprint.getId(), tracker.failures().get(0).getId());
        assertInvalidMetadataRejected(tracker, fingerprint);
        tracker.clear();
        assertCleared(tracker);
    }

    private static void assertInvalidMetadataRejected(
            ConsumerFailureTracker tracker,
            Fingerprint fingerprint
    ) {
        assertThrows(
                NullPointerException.class,
                () -> tracker.capture(null, 0, 0, fingerprint)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.capture(" ", 0, 0, fingerprint)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.capture("payment-events", -1, 0, fingerprint)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.capture("payment-events", 0, -1, fingerprint)
        );
        assertThrows(
                NullPointerException.class,
                () -> tracker.capture("payment-events", 0, 0, (Fingerprint) null)
        );
    }

    private static void assertCleared(ConsumerFailureTracker tracker) {
        assertEquals(0, tracker.failures().size());
        assertEquals("", tracker.report());
    }

    @Test
    void safelyCountsConcurrentConsumerFailures() throws InterruptedException {
        ConsumerFailureTracker tracker = new ConsumerFailureTracker();
        Fingerprint fingerprint = fingerprintAt(
                "com.example.PaymentConsumer",
                "consume",
                10
        );
        ExecutorService executor = Executors.newFixedThreadPool(8);

        for (int i = 0; i < 1000; i++) {
            final long offset = i;
            executor.submit(() -> tracker.capture(
                    "payment-events",
                    (int) (offset % 4),
                    offset,
                    fingerprint
            ));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, tracker.failures().size());
        assertEquals(1000, tracker.failures().get(0).getOccurrences());
    }

    private static Fingerprint fingerprintAt(
            String className,
            String methodName,
            int lineNumber
    ) {
        return BugDna.generate(failureAt(className, methodName, lineNumber));
    }

    private static Throwable failureAt(String className, String methodName, int lineNumber) {
        RuntimeException failure = new RuntimeException();
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(className, methodName, className + ".java", lineNumber)
        });
        return failure;
    }
}
