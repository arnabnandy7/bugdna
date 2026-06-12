package io.github.bugdna.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Immutable fingerprint occurrence analysis for one log file.
 */
public final class LogAnalysis {

    private final List<FailureCount> failures;

    LogAnalysis(Map<String, Long> occurrences) {
        List<FailureCount> sortedFailures = new ArrayList<>();
        for (Map.Entry<String, Long> occurrence : occurrences.entrySet()) {
            sortedFailures.add(new FailureCount(
                    occurrence.getKey(),
                    occurrence.getValue()
            ));
        }
        sortedFailures.sort(Comparator
                .comparingLong(FailureCount::getOccurrences)
                .reversed()
                .thenComparing(FailureCount::getId));
        this.failures = Collections.unmodifiableList(sortedFailures);
    }

    /**
     * Returns the number of unique fingerprint IDs found.
     *
     * @return unique fingerprint count
     */
    public int getUniqueFailures() {
        return failures.size();
    }

    /**
     * Returns immutable failure counts ordered by occurrence count.
     *
     * @return sorted failure counts
     */
    public List<FailureCount> getFailures() {
        return failures;
    }

    /**
     * Formats the command-line report.
     *
     * @return formatted analysis
     */
    public String report() {
        StringBuilder report = new StringBuilder("Unique Failures: ")
                .append(getUniqueFailures());
        if (!failures.isEmpty()) {
            report.append(System.lineSeparator());
        }
        for (FailureCount failure : failures) {
            report.append(System.lineSeparator())
                    .append(failure.getId())
                    .append(" : ")
                    .append(failure.getOccurrences());
        }
        return report.toString();
    }

    /**
     * Immutable count for one fingerprint ID.
     */
    public static final class FailureCount {

        private final String id;
        private final long occurrences;

        private FailureCount(String id, long occurrences) {
            this.id = id;
            this.occurrences = occurrences;
        }

        /**
         * Returns the fingerprint ID.
         *
         * @return fingerprint ID
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the number of log occurrences.
         *
         * @return occurrence count
         */
        public long getOccurrences() {
            return occurrences;
        }
    }
}
