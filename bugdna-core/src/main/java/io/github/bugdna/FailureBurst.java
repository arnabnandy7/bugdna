package io.github.bugdna;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable burst summary calculated from a retained failure timeline.
 */
public final class FailureBurst {

    private final Fingerprint fingerprint;
    private final Instant firstSeen;
    private final Instant lastSeen;
    private final long peakRatePerMinute;
    private final long occurrences;

    FailureBurst(
            Fingerprint fingerprint,
            Instant firstSeen,
            Instant lastSeen,
            long peakRatePerMinute,
            long occurrences
    ) {
        this.fingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint must not be null"
        );
        this.firstSeen = Objects.requireNonNull(firstSeen, "firstSeen must not be null");
        this.lastSeen = Objects.requireNonNull(lastSeen, "lastSeen must not be null");
        this.peakRatePerMinute = peakRatePerMinute;
        this.occurrences = occurrences;
    }

    /**
     * Returns the burst fingerprint.
     *
     * @return failure fingerprint
     */
    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    /**
     * Returns the fingerprint ID.
     *
     * @return fingerprint ID
     */
    public String getId() {
        return fingerprint.getId();
    }

    /**
     * Returns the first retained occurrence.
     *
     * @return first-seen timestamp
     */
    public Instant getFirstSeen() {
        return firstSeen;
    }

    /**
     * Returns the last retained occurrence.
     *
     * @return last-seen timestamp
     */
    public Instant getLastSeen() {
        return lastSeen;
    }

    /**
     * Returns the highest number of occurrences in one UTC minute.
     *
     * @return peak occurrences per minute
     */
    public long getPeakRatePerMinute() {
        return peakRatePerMinute;
    }

    /**
     * Returns the number of retained occurrences in the burst summary.
     *
     * @return retained occurrence count
     */
    public long getOccurrences() {
        return occurrences;
    }

    /**
     * Returns elapsed time from first seen to last seen.
     *
     * @return burst duration
     */
    public Duration getDuration() {
        return Duration.between(firstSeen, lastSeen);
    }

    /**
     * Formats the burst as a compact report using ISO-8601 timestamps.
     *
     * @return burst report
     */
    public String report() {
        return getId()
                + " burst detected"
                + System.lineSeparator()
                + System.lineSeparator()
                + "First Seen: "
                + firstSeen
                + System.lineSeparator()
                + "Peak Rate: "
                + peakRatePerMinute
                + "/min"
                + System.lineSeparator()
                + "Duration: "
                + formatDuration(getDuration());
    }

    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds % 60 == 0) {
            return (seconds / 60) + " min";
        }
        return seconds + " sec";
    }
}
