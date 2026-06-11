package io.github.bugdna;

import java.util.Objects;

/**
 * Immutable occurrence count for one failure fingerprint.
 */
public final class FailureAggregate {

    private final Fingerprint fingerprint;
    private final long occurrences;

    FailureAggregate(Fingerprint fingerprint, long occurrences) {
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        this.occurrences = occurrences;
    }

    /**
     * Returns the aggregated fingerprint.
     *
     * @return failure fingerprint
     */
    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    /**
     * Returns the fingerprint id.
     *
     * @return fingerprint id
     */
    public String getId() {
        return fingerprint.getId();
    }

    /**
     * Returns the number of captured occurrences.
     *
     * @return occurrence count
     */
    public long getOccurrences() {
        return occurrences;
    }
}
