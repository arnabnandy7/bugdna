package io.github.bugdna;

import java.util.Objects;

/**
 * Immutable aggregate for one consumer topic and failure fingerprint.
 */
public final class ConsumerFailureAggregate {

    private final String topic;
    private final int partition;
    private final long offset;
    private final Fingerprint fingerprint;
    private final long occurrences;

    ConsumerFailureAggregate(
            String topic,
            int partition,
            long offset,
            Fingerprint fingerprint,
            long occurrences
    ) {
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        this.partition = partition;
        this.offset = offset;
        this.fingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint must not be null"
        );
        this.occurrences = occurrences;
    }

    /**
     * Returns the consumer topic.
     *
     * @return topic name
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Returns the partition from the latest captured occurrence.
     *
     * @return latest partition
     */
    public int getPartition() {
        return partition;
    }

    /**
     * Returns the offset from the latest captured occurrence.
     *
     * @return latest offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Returns the grouped failure fingerprint.
     *
     * @return failure fingerprint
     */
    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    /**
     * Returns the fingerprint identifier.
     *
     * @return fingerprint identifier
     */
    public String getId() {
        return fingerprint.getId();
    }

    /**
     * Returns the number of captured consumer failures.
     *
     * @return occurrence count
     */
    public long getOccurrences() {
        return occurrences;
    }
}
