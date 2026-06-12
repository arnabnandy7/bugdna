package io.github.bugdna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe, in-memory aggregator for recurring failure fingerprints.
 */
public final class FailureTracker {

    private static final int DEFAULT_TOP_FAILURE_LIMIT = 10;
    private static final int DEFAULT_TOP_FAMILY_LIMIT = 10;

    private final ConcurrentHashMap<String, TrackedFailure> failures = new ConcurrentHashMap<>();
    private final LongAdder totalOccurrences = new LongAdder();

    /**
     * Fingerprints and records a failure.
     *
     * @param failure failure to capture
     * @return generated fingerprint
     */
    public Fingerprint capture(Throwable failure) {
        Fingerprint fingerprint = BugDna.generate(failure);
        capture(fingerprint);
        return fingerprint;
    }

    /**
     * Records an existing fingerprint.
     *
     * @param fingerprint fingerprint to capture
     */
    public void capture(Fingerprint fingerprint) {
        Fingerprint requiredFingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint must not be null"
        );
        failures.computeIfAbsent(
                requiredFingerprint.getId(),
                ignored -> new TrackedFailure(requiredFingerprint)
        ).increment();
        totalOccurrences.increment();
    }

    /**
     * Returns immutable aggregates sorted by occurrence count, highest first.
     *
     * @return current failure aggregates
     */
    public List<FailureAggregate> failures() {
        List<FailureAggregate> snapshot = new ArrayList<>();
        for (TrackedFailure failure : failures.values()) {
            snapshot.add(failure.snapshot());
        }
        snapshot.sort(Comparator
                .comparingLong(FailureAggregate::getOccurrences)
                .reversed()
                .thenComparing(FailureAggregate::getId));
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Returns the most frequent failures, highest occurrence count first.
     *
     * @param limit maximum number of failures to return
     * @return immutable top-failure list
     */
    public List<FailureAggregate> topFailures(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        List<FailureAggregate> sortedFailures = failures();
        int resultSize = Math.min(limit, sortedFailures.size());
        return Collections.unmodifiableList(new ArrayList<>(
                sortedFailures.subList(0, resultSize)
        ));
    }

    /**
     * Returns immutable root-cause family aggregates, highest occurrence count first.
     *
     * @return current family aggregates
     */
    public List<FailureFamilyAggregate> families() {
        Map<FailureFamily, List<FailureAggregate>> grouped =
                new EnumMap<>(FailureFamily.class);
        Map<FailureFamily, Long> occurrences = new EnumMap<>(FailureFamily.class);

        for (FailureAggregate failure : failures()) {
            FailureFamily family = failure.getFingerprint().getFamily();
            grouped.computeIfAbsent(family, ignored -> new ArrayList<>()).add(failure);
            occurrences.put(
                    family,
                    occurrences.getOrDefault(family, 0L) + failure.getOccurrences()
            );
        }

        List<FailureFamilyAggregate> snapshot = new ArrayList<>();
        for (Map.Entry<FailureFamily, List<FailureAggregate>> entry : grouped.entrySet()) {
            snapshot.add(new FailureFamilyAggregate(
                    entry.getKey(),
                    entry.getValue(),
                    occurrences.get(entry.getKey())
            ));
        }
        snapshot.sort(Comparator
                .comparingLong(FailureFamilyAggregate::getOccurrences)
                .reversed()
                .thenComparing(aggregate -> aggregate.getFamily().name()));
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Returns the most frequent root-cause families.
     *
     * @param limit maximum number of families to return
     * @return immutable top-family list
     */
    public List<FailureFamilyAggregate> topFamilies(int limit) {
        validateLimit(limit);
        List<FailureFamilyAggregate> sortedFamilies = families();
        int resultSize = Math.min(limit, sortedFamilies.size());
        return Collections.unmodifiableList(new ArrayList<>(
                sortedFamilies.subList(0, resultSize)
        ));
    }

    /**
     * Returns the total number of captured failures.
     *
     * @return total occurrence count
     */
    public long getTotalOccurrences() {
        return totalOccurrences.sum();
    }

    /**
     * Returns the number of unique failure fingerprints.
     *
     * @return unique failure count
     */
    public int getUniqueFailures() {
        return failures.size();
    }

    /**
     * Returns the number of operational root-cause families.
     *
     * @return unique family count
     */
    public int getUniqueFamilies() {
        return families().size();
    }

    /**
     * Formats the current aggregates as a compact report.
     *
     * @return failure occurrence report
     */
    public String report() {
        List<FailureAggregate> groupedFailures = failures();
        StringBuilder report = new StringBuilder()
                .append(groupedFailures.size())
                .append(groupedFailures.size() == 1
                        ? " unique failure signature"
                        : " unique failure signatures");
        for (FailureAggregate failure : groupedFailures) {
            report.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append(failure.getId())
                    .append(System.lineSeparator())
                    .append("Count: ")
                    .append(failure.getOccurrences());
        }
        return report.toString();
    }

    /**
     * Formats the ten most frequent failure signatures.
     *
     * @return top-ten failure report
     */
    public String topFailureReport() {
        return topFailureReport(DEFAULT_TOP_FAILURE_LIMIT);
    }

    /**
     * Formats the most frequent failure signatures.
     *
     * @param limit maximum number of failures to include
     * @return top-failure report
     */
    public String topFailureReport(int limit) {
        StringBuilder report = new StringBuilder("Top ")
                .append(limit)
                .append(" Failure Signatures");
        for (FailureAggregate failure : topFailures(limit)) {
            report.append(System.lineSeparator())
                    .append(failure.getId())
                    .append(System.lineSeparator())
                    .append("Count: ")
                    .append(failure.getOccurrences());
        }
        return report.toString();
    }

    /**
     * Formats all root-cause families and their member fingerprints.
     *
     * @return root-cause family report
     */
    public String familyReport() {
        return formatFamilyReport("Root Cause Families", families());
    }

    /**
     * Formats the ten most frequent root-cause families.
     *
     * @return top-ten family report
     */
    public String topFamilyReport() {
        return topFamilyReport(DEFAULT_TOP_FAMILY_LIMIT);
    }

    /**
     * Formats the most frequent root-cause families.
     *
     * @param limit maximum number of families to include
     * @return top-family report
     */
    public String topFamilyReport(int limit) {
        validateLimit(limit);
        return formatFamilyReport(
                "Top " + limit + " Root Cause Families",
                topFamilies(limit)
        );
    }

    /**
     * Removes all captured failure counts.
     */
    public void clear() {
        failures.clear();
        totalOccurrences.reset();
    }

    private static String formatFamilyReport(
            String heading,
            List<FailureFamilyAggregate> families
    ) {
        StringBuilder report = new StringBuilder(heading);
        for (FailureFamilyAggregate family : families) {
            report.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("Family: ")
                    .append(family.getFamily().name())
                    .append(System.lineSeparator())
                    .append("Occurrences: ")
                    .append(family.getOccurrences())
                    .append(System.lineSeparator())
                    .append("Unique Failures: ")
                    .append(family.getUniqueFailures());
            for (FailureAggregate failure : family.getFailures()) {
                report.append(System.lineSeparator())
                        .append(failure.getId())
                        .append(" (")
                        .append(failure.getOccurrences())
                        .append(')');
            }
        }
        return report.toString();
    }

    private static void validateLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
    }

    private static final class TrackedFailure {

        private final Fingerprint fingerprint;
        private final LongAdder occurrences = new LongAdder();

        private TrackedFailure(Fingerprint fingerprint) {
            this.fingerprint = fingerprint;
        }

        private void increment() {
            occurrences.increment();
        }

        private FailureAggregate snapshot() {
            return new FailureAggregate(fingerprint, occurrences.sum());
        }
    }
}
