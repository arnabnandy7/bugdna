package io.github.bugdna;

import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
    private static final int DEFAULT_TIMELINE_LIMIT = 10_000;
    private static final Duration DEFAULT_BURST_MAX_IDLE_GAP = Duration.ofMinutes(1);
    private static final DateTimeFormatter TIMELINE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final ConcurrentHashMap<String, TrackedFailure> failures = new ConcurrentHashMap<>();
    private final LongAdder totalOccurrences = new LongAdder();
    private final Object timelineLock = new Object();
    private final ArrayDeque<FailureOccurrence> timeline = new ArrayDeque<>();
    private final int timelineLimit;

    /**
     * Creates a tracker retaining up to 10,000 timestamped occurrences.
     */
    public FailureTracker() {
        this(DEFAULT_TIMELINE_LIMIT);
    }

    /**
     * Creates a tracker with bounded timestamped occurrence retention.
     *
     * @param timelineLimit maximum retained timeline events
     */
    public FailureTracker(int timelineLimit) {
        if (timelineLimit < 1) {
            throw new IllegalArgumentException("timelineLimit must be at least 1");
        }
        this.timelineLimit = timelineLimit;
    }

    /**
     * Fingerprints and records a failure.
     *
     * @param failure failure to capture
     * @return generated fingerprint
     */
    public Fingerprint capture(Throwable failure) {
        return capture(failure, Instant.now());
    }

    /**
     * Fingerprints and records a failure at an explicit timestamp.
     *
     * @param failure failure to capture
     * @param occurredAt occurrence timestamp
     * @return generated fingerprint
     */
    public Fingerprint capture(Throwable failure, Instant occurredAt) {
        Fingerprint fingerprint = BugDna.generate(failure);
        capture(fingerprint, occurredAt);
        return fingerprint;
    }

    /**
     * Records an existing fingerprint.
     *
     * @param fingerprint fingerprint to capture
     */
    public void capture(Fingerprint fingerprint) {
        capture(fingerprint, Instant.now());
    }

    /**
     * Records an existing fingerprint at an explicit timestamp.
     *
     * @param fingerprint fingerprint to capture
     * @param occurredAt occurrence timestamp
     */
    public void capture(Fingerprint fingerprint, Instant occurredAt) {
        Fingerprint requiredFingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint must not be null"
        );
        Instant requiredTimestamp = Objects.requireNonNull(
                occurredAt,
                "occurredAt must not be null"
        );
        failures.computeIfAbsent(
                requiredFingerprint.getId(),
                ignored -> new TrackedFailure(requiredFingerprint)
        ).increment();
        totalOccurrences.increment();
        recordTimeline(requiredFingerprint, requiredTimestamp);
    }

    /**
     * Returns retained timestamped occurrences in chronological order.
     *
     * @return immutable timeline snapshot
     */
    public List<FailureOccurrence> timeline() {
        List<FailureOccurrence> snapshot;
        synchronized (timelineLock) {
            snapshot = new ArrayList<>(timeline);
        }
        snapshot.sort(Comparator
                .comparing(FailureOccurrence::getOccurredAt)
                .thenComparing(FailureOccurrence::getId));
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Formats retained occurrences using the supplied time zone.
     *
     * @param zone time zone used for display
     * @return compact timeline report
     */
    public String timelineReport(ZoneId zone) {
        Objects.requireNonNull(zone, "zone must not be null");
        StringBuilder report = new StringBuilder();
        boolean first = true;
        for (FailureOccurrence occurrence : timeline()) {
            if (first) {
                first = false;
            } else {
                report.append(System.lineSeparator());
            }
            report.append(TIMELINE_TIME_FORMAT
                            .withZone(zone)
                            .format(occurrence.getOccurredAt()))
                    .append(' ')
                    .append(occurrence.getId());
        }
        return report.toString();
    }

    /**
     * Finds fingerprints whose peak retained one-minute rate meets the threshold.
     *
     * @param minimumPeakRatePerMinute minimum occurrences in one UTC minute
     * @return immutable burst summaries, highest peak first
     */
    public List<FailureBurst> bursts(long minimumPeakRatePerMinute) {
        return bursts(minimumPeakRatePerMinute, DEFAULT_BURST_MAX_IDLE_GAP);
    }

    /**
     * Finds contiguous fingerprint bursts using a caller-defined idle boundary.
     *
     * @param minimumPeakRatePerMinute minimum occurrences in one UTC minute
     * @param maximumIdleGap maximum gap between occurrences in one burst
     * @return immutable burst summaries, highest peak first
     */
    public List<FailureBurst> bursts(
            long minimumPeakRatePerMinute,
            Duration maximumIdleGap
    ) {
        if (minimumPeakRatePerMinute < 1) {
            throw new IllegalArgumentException(
                    "minimumPeakRatePerMinute must be at least 1"
            );
        }
        Objects.requireNonNull(maximumIdleGap, "maximumIdleGap must not be null");
        if (maximumIdleGap.isNegative() || maximumIdleGap.isZero()) {
            throw new IllegalArgumentException("maximumIdleGap must be positive");
        }
        Map<String, BurstAccumulator> accumulators = new HashMap<>();
        List<FailureBurst> bursts = new ArrayList<>();
        for (FailureOccurrence occurrence : timeline()) {
            BurstAccumulator accumulator = accumulators.get(occurrence.getId());
            if (accumulator != null
                    && accumulator.gapBefore(occurrence.getOccurredAt())
                            .compareTo(maximumIdleGap) > 0) {
                addBurstIfQualified(
                        bursts,
                        accumulator.snapshot(),
                        minimumPeakRatePerMinute
                );
                accumulator = null;
            }
            if (accumulator == null) {
                accumulator = new BurstAccumulator(occurrence.getFingerprint());
                accumulators.put(occurrence.getId(), accumulator);
            }
            accumulator.add(occurrence.getOccurredAt());
        }

        for (BurstAccumulator accumulator : accumulators.values()) {
            addBurstIfQualified(
                    bursts,
                    accumulator.snapshot(),
                    minimumPeakRatePerMinute
            );
        }
        bursts.sort(Comparator
                .comparingLong(FailureBurst::getPeakRatePerMinute)
                .reversed()
                .thenComparing(FailureBurst::getId));
        return Collections.unmodifiableList(bursts);
    }

    /**
     * Formats retained burst summaries using the supplied time zone.
     *
     * @param minimumPeakRatePerMinute minimum occurrences in one UTC minute
     * @param zone time zone used for display
     * @return burst report
     */
    public String burstReport(long minimumPeakRatePerMinute, ZoneId zone) {
        Objects.requireNonNull(zone, "zone must not be null");
        StringBuilder report = new StringBuilder();
        boolean first = true;
        for (FailureBurst burst : bursts(minimumPeakRatePerMinute)) {
            if (first) {
                first = false;
            } else {
                report.append(System.lineSeparator()).append(System.lineSeparator());
            }
            report.append(burst.getId())
                    .append(" burst detected")
                    .append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("First Seen: ")
                    .append(TIMELINE_TIME_FORMAT.withZone(zone).format(burst.getFirstSeen()))
                    .append(System.lineSeparator())
                    .append("Peak Rate: ")
                    .append(burst.getPeakRatePerMinute())
                    .append("/min")
                    .append(System.lineSeparator())
                    .append("Duration: ")
                    .append(formatDuration(burst));
        }
        return report.toString();
    }

    /**
     * Returns the maximum number of retained timeline events.
     *
     * @return timeline retention limit
     */
    public int getTimelineLimit() {
        return timelineLimit;
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
        synchronized (timelineLock) {
            timeline.clear();
        }
    }

    private void recordTimeline(Fingerprint fingerprint, Instant occurredAt) {
        synchronized (timelineLock) {
            timeline.addLast(new FailureOccurrence(occurredAt, fingerprint));
            while (timeline.size() > timelineLimit) {
                timeline.removeFirst();
            }
        }
    }

    private static String formatDuration(FailureBurst burst) {
        long seconds = burst.getDuration().getSeconds();
        if (seconds % 60 == 0) {
            return (seconds / 60) + " min";
        }
        return seconds + " sec";
    }

    private static void addBurstIfQualified(
            List<FailureBurst> bursts,
            FailureBurst burst,
            long minimumPeakRatePerMinute
    ) {
        if (burst.getPeakRatePerMinute() >= minimumPeakRatePerMinute) {
            bursts.add(burst);
        }
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

    private static final class BurstAccumulator {

        private final Fingerprint fingerprint;
        private final Map<Long, Long> minuteCounts = new HashMap<>();
        private Instant firstSeen;
        private Instant lastSeen;
        private long occurrences;
        private long peakRatePerMinute;

        private BurstAccumulator(Fingerprint fingerprint) {
            this.fingerprint = fingerprint;
        }

        private void add(Instant occurredAt) {
            if (firstSeen == null || occurredAt.isBefore(firstSeen)) {
                firstSeen = occurredAt;
            }
            if (lastSeen == null || occurredAt.isAfter(lastSeen)) {
                lastSeen = occurredAt;
            }
            occurrences++;
            long minute = Math.floorDiv(occurredAt.getEpochSecond(), 60);
            long minuteCount = minuteCounts.getOrDefault(minute, 0L) + 1;
            minuteCounts.put(minute, minuteCount);
            peakRatePerMinute = Math.max(peakRatePerMinute, minuteCount);
        }

        private Duration gapBefore(Instant occurredAt) {
            return Duration.between(lastSeen, occurredAt);
        }

        private FailureBurst snapshot() {
            return new FailureBurst(
                    fingerprint,
                    firstSeen,
                    lastSeen,
                    peakRatePerMinute,
                    occurrences
            );
        }
    }
}
