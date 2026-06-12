package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
                "2 unique failure signatures"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + frequentFingerprint.getId()
                        + System.lineSeparator()
                        + "Count: 2"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + occasionalFingerprint.getId()
                        + System.lineSeparator()
                        + "Count: 1",
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
    void generatesBoundedTopFailureReport() {
        FailureTracker tracker = new FailureTracker();
        Fingerprint first = fingerprintAt("com.example.FirstJob", "run", 10);
        Fingerprint second = fingerprintAt("com.example.SecondJob", "run", 20);
        Fingerprint third = fingerprintAt("com.example.ThirdJob", "run", 30);

        capture(tracker, first, 3);
        capture(tracker, second, 5);
        capture(tracker, third, 1);

        List<FailureAggregate> topFailures = tracker.topFailures(2);
        assertEquals(2, topFailures.size());
        assertEquals(second.getId(), topFailures.get(0).getId());
        assertEquals(first.getId(), topFailures.get(1).getId());
        assertEquals(
                "Top 2 Failure Signatures"
                        + System.lineSeparator()
                        + second.getId()
                        + System.lineSeparator()
                        + "Count: 5"
                        + System.lineSeparator()
                        + first.getId()
                        + System.lineSeparator()
                        + "Count: 3",
                tracker.topFailureReport(2)
        );
        assertTrue(tracker.topFailureReport().startsWith("Top 10 Failure Signatures"));
        assertThrows(IllegalArgumentException.class, () -> tracker.topFailures(0));
        assertThrows(IllegalArgumentException.class, () -> tracker.topFailureReport(0));
    }

    @Test
    void clustersDifferentFingerprintsIntoOneRootCauseFamily() {
        FailureTracker tracker = new FailureTracker();
        Fingerprint refused = tracker.capture(failureAt(
                new ConnectException("Connection refused"),
                "org.postgresql.core.PGStream",
                "createSocket",
                10
        ));
        Fingerprint timedOut = tracker.capture(failureAt(
                new SocketTimeoutException("Socket timeout"),
                "com.zaxxer.hikari.pool.PoolBase",
                "newConnection",
                20
        ));
        Fingerprint exhausted = tracker.capture(failureAt(
                new SQLTransientConnectionException("Pool exhausted"),
                "com.example.UserRepository",
                "find",
                30
        ));
        tracker.capture(exhausted);

        List<FailureFamilyAggregate> families = tracker.families();
        assertEquals(1, tracker.getUniqueFamilies());
        assertEquals(1, families.size());
        assertEquals(FailureFamily.DATABASE_CONNECTIVITY, families.get(0).getFamily());
        assertEquals(4, families.get(0).getOccurrences());
        assertEquals(3, families.get(0).getUniqueFailures());
        assertEquals(exhausted.getId(), families.get(0).getFailures().get(0).getId());
        assertTrue(families.get(0).getFailures().stream()
                .anyMatch(failure -> failure.getId().equals(refused.getId())));
        assertTrue(families.get(0).getFailures().stream()
                .anyMatch(failure -> failure.getId().equals(timedOut.getId())));
        assertTrue(tracker.familyReport().contains("Family: DATABASE_CONNECTIVITY"));
        assertTrue(tracker.familyReport().contains("Unique Failures: 3"));
        assertTrue(tracker.topFamilyReport(1).startsWith("Top 1 Root Cause Families"));
        assertThrows(UnsupportedOperationException.class, families::clear);
        assertThrows(
                UnsupportedOperationException.class,
                () -> families.get(0).getFailures().clear()
        );
        assertThrows(IllegalArgumentException.class, () -> tracker.topFamilies(0));
        assertThrows(IllegalArgumentException.class, () -> tracker.topFamilyReport(0));
    }

    @Test
    void recordsAndFormatsFailureTimelineInChronologicalOrder() {
        FailureTracker tracker = new FailureTracker();
        Fingerprint first = fingerprintAt("com.example.FirstJob", "run", 10);
        Fingerprint second = fingerprintAt("com.example.SecondJob", "run", 20);

        tracker.capture(first, Instant.parse("2026-06-13T09:04:00Z"));
        tracker.capture(first, Instant.parse("2026-06-13T09:01:00Z"));
        tracker.capture(first, Instant.parse("2026-06-13T09:02:00Z"));
        tracker.capture(second, Instant.parse("2026-06-13T09:03:00Z"));

        List<FailureOccurrence> timeline = tracker.timeline();
        assertEquals(4, timeline.size());
        assertEquals(Instant.parse("2026-06-13T09:01:00Z"), timeline.get(0).getOccurredAt());
        assertEquals(first.getId(), timeline.get(0).getId());
        assertEquals(second.getId(), timeline.get(2).getId());
        assertEquals(
                "09:01 " + first.getId()
                        + System.lineSeparator()
                        + "09:02 " + first.getId()
                        + System.lineSeparator()
                        + "09:03 " + second.getId()
                        + System.lineSeparator()
                        + "09:04 " + first.getId(),
                tracker.timelineReport(ZoneOffset.UTC)
        );
        assertThrows(UnsupportedOperationException.class, timeline::clear);
    }

    @Test
    void detectsPeakMinuteRateAndBurstDuration() {
        FailureTracker tracker = new FailureTracker();
        Fingerprint fingerprint = fingerprintAt("com.example.DatabaseClient", "connect", 10);
        Instant firstSeen = Instant.parse("2026-06-13T09:01:00Z");

        tracker.capture(fingerprint, firstSeen);
        for (int i = 0; i < 312; i++) {
            tracker.capture(
                    fingerprint,
                    Instant.parse("2026-06-13T09:02:00Z").plusMillis(i)
            );
        }
        for (int minute = 3; minute <= 23; minute++) {
            tracker.capture(
                    fingerprint,
                    Instant.parse("2026-06-13T09:00:00Z").plus(Duration.ofMinutes(minute))
            );
        }
        tracker.capture(fingerprint, Instant.parse("2026-06-13T10:00:00Z"));

        List<FailureBurst> bursts = tracker.bursts(300);
        assertEquals(1, bursts.size());
        assertEquals(fingerprint.getId(), bursts.get(0).getId());
        assertEquals(firstSeen, bursts.get(0).getFirstSeen());
        assertEquals(312, bursts.get(0).getPeakRatePerMinute());
        assertEquals(334, bursts.get(0).getOccurrences());
        assertEquals(Duration.ofMinutes(22), bursts.get(0).getDuration());
        assertEquals(
                fingerprint.getId()
                        + " burst detected"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "First Seen: 09:01"
                        + System.lineSeparator()
                        + "Peak Rate: 312/min"
                        + System.lineSeparator()
                        + "Duration: 22 min",
                tracker.burstReport(300, ZoneOffset.UTC)
        );
        assertTrue(tracker.bursts(313).isEmpty());
    }

    @Test
    void boundsTimelineRetentionWithoutChangingLifetimeCounts() {
        FailureTracker tracker = new FailureTracker(2);
        Fingerprint fingerprint = fingerprintAt("com.example.Job", "run", 10);

        tracker.capture(fingerprint, Instant.parse("2026-06-13T09:01:00Z"));
        tracker.capture(fingerprint, Instant.parse("2026-06-13T09:02:00Z"));
        tracker.capture(fingerprint, Instant.parse("2026-06-13T09:03:00Z"));

        assertEquals(2, tracker.getTimelineLimit());
        assertEquals(2, tracker.timeline().size());
        assertEquals(Instant.parse("2026-06-13T09:02:00Z"), tracker.timeline().get(0).getOccurredAt());
        assertEquals(3, tracker.getTotalOccurrences());

        tracker.clear();
        assertTrue(tracker.timeline().isEmpty());
    }

    @Test
    void rejectsNullCaptures() {
        FailureTracker tracker = new FailureTracker();

        assertThrows(NullPointerException.class, () -> tracker.capture((Throwable) null));
        assertThrows(NullPointerException.class, () -> tracker.capture((Fingerprint) null));
        assertThrows(
                NullPointerException.class,
                () -> tracker.capture(
                        fingerprintAt("com.example.Job", "run", 10),
                        null
                )
        );
        assertThrows(NullPointerException.class, () -> tracker.timelineReport(null));
        assertThrows(NullPointerException.class, () -> tracker.burstReport(1, null));
        assertThrows(NullPointerException.class, () -> tracker.bursts(1, null));
        assertThrows(IllegalArgumentException.class, () -> new FailureTracker(0));
        assertThrows(IllegalArgumentException.class, () -> tracker.bursts(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.bursts(1, Duration.ZERO)
        );
    }

    @Test
    void reportsSingularAndEmptySignatureCounts() {
        FailureTracker tracker = new FailureTracker();

        assertEquals("0 unique failure signatures", tracker.report());

        tracker.capture(failureAt("com.example.UserService", "load", 10));

        assertTrue(tracker.report().startsWith(
                "1 unique failure signature" + System.lineSeparator()
        ));
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

    private static Fingerprint fingerprintAt(
            String className,
            String methodName,
            int lineNumber
    ) {
        return BugDna.generate(failureAt(className, methodName, lineNumber));
    }

    private static void capture(FailureTracker tracker, Fingerprint fingerprint, int count) {
        for (int i = 0; i < count; i++) {
            tracker.capture(fingerprint);
        }
    }
}
