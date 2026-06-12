package io.github.bugdna.cli;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Compares fingerprint signatures found in two log analyses.
 */
public final class LogComparator {

    /**
     * Compares an older log analysis with a newer one.
     *
     * @param oldAnalysis older log analysis
     * @param newAnalysis newer log analysis
     * @return signature comparison
     */
    public LogComparison compare(LogAnalysis oldAnalysis, LogAnalysis newAnalysis) {
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

        return new LogComparison(newFailures.size(), resolvedFailures.size());
    }

    private static Set<String> fingerprintIds(LogAnalysis analysis) {
        Set<String> ids = new HashSet<>();
        for (LogAnalysis.FailureCount failure : analysis.getFailures()) {
            ids.add(failure.getId());
        }
        return ids;
    }
}
