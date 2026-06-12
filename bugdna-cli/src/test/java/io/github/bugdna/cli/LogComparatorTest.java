package io.github.bugdna.cli;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogComparatorTest {

    @Test
    void countsNewAndResolvedSignatures() {
        LogAnalysis oldAnalysis = analysis(
                "BUGDNA-001",
                "BUGDNA-002",
                "BUGDNA-003"
        );
        LogAnalysis newAnalysis = analysis(
                "BUGDNA-002",
                "BUGDNA-004",
                "BUGDNA-005"
        );

        LogComparison comparison = new LogComparator().compare(
                oldAnalysis,
                newAnalysis
        );

        assertEquals(2, comparison.getNewFailureSignatures());
        assertEquals(2, comparison.getResolvedFailureSignatures());
        assertEquals(1, comparison.getRecurringFailureSignatures());
        assertEquals("BUGDNA-004", comparison.getNewFingerprints().get(0));
        assertEquals("BUGDNA-002", comparison.getRecurringFingerprints().get(0));
        assertEquals(
                "New fingerprints: 2"
                        + System.lineSeparator()
                        + "Resolved fingerprints: 2"
                        + System.lineSeparator()
                        + "Recurring fingerprints: 1",
                comparison.report()
        );
    }

    @Test
    void ignoresOccurrenceCountChangesForSharedSignatures() {
        Map<String, Long> oldOccurrences = new HashMap<>();
        oldOccurrences.put("BUGDNA-001", 1L);
        Map<String, Long> newOccurrences = new HashMap<>();
        newOccurrences.put("BUGDNA-001", 500L);

        LogComparison comparison = new LogComparator().compare(
                new LogAnalysis(oldOccurrences),
                new LogAnalysis(newOccurrences)
        );

        assertEquals(0, comparison.getNewFailureSignatures());
        assertEquals(0, comparison.getResolvedFailureSignatures());
        assertEquals(1, comparison.getRecurringFailureSignatures());
    }

    @Test
    void reportsVersionLabels() {
        LogComparison comparison = new LogComparator().compare(
                "1.2.0",
                analysis("BUGDNA-001", "BUGDNA-002"),
                "1.3.0",
                analysis("BUGDNA-002", "BUGDNA-003")
        );

        assertEquals(
                "Version 1.2.0 -> Version 1.3.0"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "New fingerprints: 1"
                        + System.lineSeparator()
                        + "Resolved fingerprints: 1"
                        + System.lineSeparator()
                        + "Recurring fingerprints: 1",
                comparison.report()
        );
    }

    @Test
    void rejectsNullAnalyses() {
        LogAnalysis analysis = analysis("BUGDNA-001");

        assertThrows(
                NullPointerException.class,
                () -> new LogComparator().compare(null, analysis)
        );
        assertThrows(
                NullPointerException.class,
                () -> new LogComparator().compare(analysis, null)
        );
    }

    private static LogAnalysis analysis(String... ids) {
        Map<String, Long> occurrences = new HashMap<>();
        for (String id : ids) {
            occurrences.put(id, 1L);
        }
        return new LogAnalysis(occurrences);
    }
}
