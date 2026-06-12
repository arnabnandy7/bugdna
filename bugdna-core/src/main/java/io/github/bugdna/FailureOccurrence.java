package io.github.bugdna;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable timestamped occurrence of one failure fingerprint.
 */
public final class FailureOccurrence {

    private final Instant occurredAt;
    private final Fingerprint fingerprint;

    FailureOccurrence(Instant occurredAt, Fingerprint fingerprint) {
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.fingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint must not be null"
        );
    }

    /**
     * Returns when the failure occurred.
     *
     * @return occurrence timestamp
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Returns the captured fingerprint.
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
}
