package io.github.bugdna.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable signature comparison between an older and newer log analysis.
 */
public final class LogComparison {

    private final String oldVersion;
    private final String newVersion;
    private final List<String> newFingerprints;
    private final List<String> resolvedFingerprints;
    private final List<String> recurringFingerprints;

    LogComparison(
            String oldVersion,
            String newVersion,
            List<String> newFingerprints,
            List<String> resolvedFingerprints,
            List<String> recurringFingerprints
    ) {
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
        this.newFingerprints = immutableCopy(newFingerprints);
        this.resolvedFingerprints = immutableCopy(resolvedFingerprints);
        this.recurringFingerprints = immutableCopy(recurringFingerprints);
    }

    /**
     * Returns signatures found only in the newer log.
     *
     * @return new signature count
     */
    public int getNewFailureSignatures() {
        return newFingerprints.size();
    }

    /**
     * Returns signatures found only in the older log.
     *
     * @return resolved signature count
     */
    public int getResolvedFailureSignatures() {
        return resolvedFingerprints.size();
    }

    /**
     * Returns signatures found in both logs.
     *
     * @return recurring signature count
     */
    public int getRecurringFailureSignatures() {
        return recurringFingerprints.size();
    }

    /**
     * Returns sorted signatures found only in the newer log.
     *
     * @return immutable new fingerprint IDs
     */
    public List<String> getNewFingerprints() {
        return newFingerprints;
    }

    /**
     * Returns sorted signatures found only in the older log.
     *
     * @return immutable resolved fingerprint IDs
     */
    public List<String> getResolvedFingerprints() {
        return resolvedFingerprints;
    }

    /**
     * Returns sorted signatures found in both logs.
     *
     * @return immutable recurring fingerprint IDs
     */
    public List<String> getRecurringFingerprints() {
        return recurringFingerprints;
    }

    /**
     * Formats the command-line comparison report.
     *
     * @return formatted comparison
     */
    public String report() {
        StringBuilder report = new StringBuilder();
        if (oldVersion != null && newVersion != null) {
            report.append("Version ")
                    .append(oldVersion)
                    .append(" -> Version ")
                    .append(newVersion)
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        return report.append("New fingerprints: ")
                .append(getNewFailureSignatures())
                .append(System.lineSeparator())
                .append("Resolved fingerprints: ")
                .append(getResolvedFailureSignatures())
                .append(System.lineSeparator())
                .append("Recurring fingerprints: ")
                .append(getRecurringFailureSignatures())
                .toString();
    }

    private static List<String> immutableCopy(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
