package io.github.bugdna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe consumer failure aggregation by topic and fingerprint.
 */
public final class ConsumerFailureTracker {

    private final ConcurrentHashMap<ConsumerFailureKey, TrackedConsumerFailure> failures =
            new ConcurrentHashMap<>();

    /**
     * Creates an empty consumer failure tracker.
     */
    public ConsumerFailureTracker() {
        // The concurrent map field is initialized eagerly.
    }

    /**
     * Fingerprints and records a consumer failure.
     *
     * @param topic consumer topic
     * @param partition consumer partition
     * @param offset consumer offset
     * @param failure consumer failure
     * @return generated fingerprint
     */
    public Fingerprint capture(
            String topic,
            int partition,
            long offset,
            Throwable failure
    ) {
        Fingerprint fingerprint = BugDna.generate(failure);
        capture(topic, partition, offset, fingerprint);
        return fingerprint;
    }

    /**
     * Records an existing fingerprint with its consumer position.
     *
     * @param topic consumer topic
     * @param partition consumer partition
     * @param offset consumer offset
     * @param fingerprint failure fingerprint
     */
    public void capture(
            String topic,
            int partition,
            long offset,
            Fingerprint fingerprint
    ) {
        String requiredTopic = validateTopic(topic);
        validatePosition(partition, offset);
        Fingerprint requiredFingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint must not be null"
        );
        ConsumerFailureKey key = new ConsumerFailureKey(
                requiredTopic,
                requiredFingerprint.getId()
        );
        failures.compute(
                key,
                (ignored, tracked) -> {
                    if (tracked == null) {
                        return new TrackedConsumerFailure(
                                requiredTopic,
                                requiredFingerprint,
                                partition,
                                offset
                        );
                    }
                    tracked.capture(partition, offset);
                    return tracked;
                }
        );
    }

    /**
     * Returns immutable aggregates sorted by occurrence count, highest first.
     *
     * @return current consumer failure aggregates
     */
    public List<ConsumerFailureAggregate> failures() {
        List<ConsumerFailureAggregate> snapshot = new ArrayList<>();
        for (TrackedConsumerFailure failure : failures.values()) {
            snapshot.add(failure.snapshot());
        }
        snapshot.sort(Comparator
                .comparingLong(ConsumerFailureAggregate::getOccurrences)
                .reversed()
                .thenComparing(ConsumerFailureAggregate::getId)
                .thenComparing(ConsumerFailureAggregate::getTopic));
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Formats all consumer failure aggregates.
     *
     * @return consumer failure report
     */
    public String report() {
        StringBuilder report = new StringBuilder();
        boolean first = true;
        for (ConsumerFailureAggregate failure : failures()) {
            if (first) {
                first = false;
            } else {
                report.append(System.lineSeparator()).append(System.lineSeparator());
            }
            report.append(failure.getId())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("Topic:")
                    .append(System.lineSeparator())
                    .append(failure.getTopic())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("Occurrences:")
                    .append(System.lineSeparator())
                    .append(failure.getOccurrences());
        }
        return report.toString();
    }

    /**
     * Removes all captured consumer failures.
     */
    public void clear() {
        failures.clear();
    }

    private static String validateTopic(String topic) {
        topic = Objects.requireNonNull(topic, "topic must not be null").trim();
        if (topic.isEmpty()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        return topic;
    }

    private static void validatePosition(int partition, long offset) {
        if (partition < 0) {
            throw new IllegalArgumentException("partition must not be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
    }

    private static final class ConsumerFailureKey {

        private final String topic;
        private final String fingerprintId;

        private ConsumerFailureKey(String topic, String fingerprintId) {
            this.topic = topic;
            this.fingerprintId = fingerprintId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ConsumerFailureKey)) {
                return false;
            }
            ConsumerFailureKey that = (ConsumerFailureKey) other;
            return topic.equals(that.topic) && fingerprintId.equals(that.fingerprintId);
        }

        @Override
        public int hashCode() {
            return 31 * topic.hashCode() + fingerprintId.hashCode();
        }
    }

    private static final class ConsumerPosition {

        private final int partition;
        private final long offset;

        private ConsumerPosition(int partition, long offset) {
            this.partition = partition;
            this.offset = offset;
        }
    }

    private static final class TrackedConsumerFailure {

        private final String topic;
        private final Fingerprint fingerprint;
        private final LongAdder occurrences = new LongAdder();
        private final AtomicReference<ConsumerPosition> latestPosition = new AtomicReference<>();

        private TrackedConsumerFailure(
                String topic,
                Fingerprint fingerprint,
                int partition,
                long offset
        ) {
            this.topic = topic;
            this.fingerprint = fingerprint;
            latestPosition.set(new ConsumerPosition(partition, offset));
            occurrences.increment();
        }

        private void capture(int partition, long offset) {
            latestPosition.set(new ConsumerPosition(partition, offset));
            occurrences.increment();
        }

        private ConsumerFailureAggregate snapshot() {
            ConsumerPosition position = latestPosition.get();
            return new ConsumerFailureAggregate(
                    topic,
                    position.partition,
                    position.offset,
                    fingerprint,
                    occurrences.sum()
            );
        }
    }
}
