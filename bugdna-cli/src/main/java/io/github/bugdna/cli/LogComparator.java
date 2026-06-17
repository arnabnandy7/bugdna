package io.github.bugdna.cli;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Compares fingerprint signatures found in two log analyses.
 */
public final class LogComparator {

    /**
     * Creates a log comparison helper.
     */
    public LogComparator() {
    }

    /**
     * Compares an older log analysis with a newer one.
     *
     * @param oldAnalysis older log analysis
     * @param newAnalysis newer log analysis
     * @return signature comparison
     */
    public LogComparison compare(LogAnalysis oldAnalysis, LogAnalysis newAnalysis) {
        return compare(null, oldAnalysis, null, newAnalysis);
    }

    /**
     * Compares version-labelled older and newer log analyses.
     *
     * @param oldVersion older deployment version
     * @param oldAnalysis older log analysis
     * @param newVersion newer deployment version
     * @param newAnalysis newer log analysis
     * @return deployment fingerprint comparison
     */
    public LogComparison compare(
            String oldVersion,
            LogAnalysis oldAnalysis,
            String newVersion,
            LogAnalysis newAnalysis
    ) {
        oldAnalysis = Objects.requireNonNull(
                oldAnalysis,
                "oldAnalysis must not be null"
        );
        newAnalysis = Objects.requireNonNull(
                newAnalysis,
                "newAnalysis must not be null"
        );

        Set<String> oldIds = fingerprintIds(oldAnalysis);
        Set<String> newIds = fingerprintIds(newAnalysis);

        Set<String> newFailures = new HashSet<>(newIds);
        newFailures.removeAll(oldIds);

        Set<String> resolvedFailures = new HashSet<>(oldIds);
        resolvedFailures.removeAll(newIds);

        Set<String> recurringFailures = new HashSet<>(oldIds);
        recurringFailures.retainAll(newIds);

        return new LogComparison(
                oldVersion,
                newVersion,
                sorted(newFailures),
                sorted(resolvedFailures),
                sorted(recurringFailures)
        );
    }

    private static Set<String> fingerprintIds(LogAnalysis analysis) {
        Set<String> ids = new HashSet<>();
        for (LogAnalysis.FailureCount failure : analysis.getFailures()) {
            ids.add(failure.getId());
        }
        return ids;
    }

    private static List<String> sorted(Set<String> ids) {
        List<String> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        return sorted;
    }
}
