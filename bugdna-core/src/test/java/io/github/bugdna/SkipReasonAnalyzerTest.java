package io.github.bugdna;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkipReasonAnalyzerTest {

    @Test
    void reportsMostCommonSkippedFailure() {
        SkipReasonAnalyzer analyzer = new SkipReasonAnalyzer();
        Fingerprint common = fingerprintAt("com.example.ImportJob", "read", 10);
        Fingerprint occasional = fingerprintAt("com.example.ImportJob", "write", 20);

        record(analyzer, common, 421);
        record(analyzer, occasional, 53);

        assertEquals(common.getId(), analyzer.getMostCommonFailure().getId());
        assertEquals(421, analyzer.getMostCommonFailure().getOccurrences());
        assertEquals(
                "Most Common Failure"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + common.getId()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Count:"
                        + System.lineSeparator()
                        + "421",
                analyzer.report()
        );
    }

    @Test
    void reportsEmptyAnalysisAndCanBeCleared() {
        SkipReasonAnalyzer analyzer = new SkipReasonAnalyzer();

        assertNull(analyzer.getMostCommonFailure());
        assertEquals(
                "Most Common Failure"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "None"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Count:"
                        + System.lineSeparator()
                        + "0",
                analyzer.report()
        );

        analyzer.record(fingerprintAt("com.example.ImportJob", "read", 10));
        analyzer.clear();

        assertNull(analyzer.getMostCommonFailure());
    }

    @Test
    void supportsExistingTrackerAndRejectsNulls() {
        FailureTracker tracker = new FailureTracker();
        SkipReasonAnalyzer analyzer = new SkipReasonAnalyzer(tracker);
        Throwable failure = failureAt("com.example.ImportJob", "read", 10);

        Fingerprint fingerprint = analyzer.record(failure);

        assertEquals(fingerprint.getId(), tracker.failures().get(0).getId());
        assertThrows(NullPointerException.class, () -> new SkipReasonAnalyzer(null));
        assertThrows(NullPointerException.class, () -> analyzer.record((Throwable) null));
        assertThrows(NullPointerException.class, () -> analyzer.record((Fingerprint) null));
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

    private static void record(
            SkipReasonAnalyzer analyzer,
            Fingerprint fingerprint,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            analyzer.record(fingerprint);
        }
    }
}
