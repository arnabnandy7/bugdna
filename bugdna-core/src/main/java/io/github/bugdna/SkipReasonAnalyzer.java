package io.github.bugdna;

import java.util.List;
import java.util.Objects;

/**
 * Groups skipped failures and identifies the most common skip reason.
 */
public final class SkipReasonAnalyzer {

    private final FailureTracker tracker;

    /**
     * Creates an analyzer with its own in-memory failure tracker.
     */
    public SkipReasonAnalyzer() {
        this(new FailureTracker());
    }

    /**
     * Creates an analyzer backed by an existing tracker.
     *
     * @param tracker tracker that receives skipped failures
     */
    public SkipReasonAnalyzer(FailureTracker tracker) {
        this.tracker = Objects.requireNonNull(tracker, "tracker must not be null");
    }

    /**
     * Fingerprints and records a skipped failure.
     *
     * @param failure skipped failure
     * @return generated fingerprint
     */
    public Fingerprint record(Throwable failure) {
        return tracker.capture(failure);
    }

    /**
     * Records an existing fingerprint as a skipped failure.
     *
     * @param fingerprint skipped failure fingerprint
     */
    public void record(Fingerprint fingerprint) {
        tracker.capture(fingerprint);
    }

    /**
     * Returns the most common skipped failure, or {@code null} when none was recorded.
     *
     * @return most common failure aggregate, or {@code null}
     */
    public FailureAggregate getMostCommonFailure() {
        List<FailureAggregate> failures = tracker.topFailures(1);
        return failures.isEmpty() ? null : failures.get(0);
    }

    /**
     * Formats the most common skipped failure.
     *
     * @return skip-reason report
     */
    public String report() {
        FailureAggregate mostCommon = getMostCommonFailure();
        String lineSeparator = System.lineSeparator();

        if (mostCommon == null) {
            return "Most Common Failure"
                    + lineSeparator
                    + lineSeparator
                    + "None"
                    + lineSeparator
                    + lineSeparator
                    + "Count:"
                    + lineSeparator
                    + "0";
        }

        return "Most Common Failure"
                + lineSeparator
                + lineSeparator
                + mostCommon.getId()
                + lineSeparator
                + lineSeparator
                + "Count:"
                + lineSeparator
                + mostCommon.getOccurrences();
    }

    /**
     * Removes all recorded skip reasons.
     */
    public void clear() {
        tracker.clear();
    }
}
