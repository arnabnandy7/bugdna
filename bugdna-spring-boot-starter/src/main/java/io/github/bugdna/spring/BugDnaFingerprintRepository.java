package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Stores a bounded list of recent fingerprints observed by the starter.
 */
public class BugDnaFingerprintRepository {

    private final int limit;
    private final LinkedList<FingerprintSnapshot> recent = new LinkedList<>();
    private final Set<String> uniqueFingerprintIds = new HashSet<>();
    private long totalCount;

    BugDnaFingerprintRepository(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        this.limit = limit;
    }

    /**
     * Records a generated fingerprint.
     *
     * @param fingerprint generated fingerprint
     */
    public synchronized void records(Fingerprint fingerprint) {
        Fingerprint requiredFingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint must not be null"
        );
        recent.addFirst(new FingerprintSnapshot(requiredFingerprint));
        totalCount++;
        uniqueFingerprintIds.add(requiredFingerprint.getId());
        while (recent.size() > limit) {
            recent.removeLast();
        }
    }

    /**
     * Returns recent fingerprints, newest first.
     *
     * @return immutable recent fingerprint list
     */
    public synchronized List<FingerprintSnapshot> recent() {
        return Collections.unmodifiableList(new ArrayList<>(recent));
    }

    /**
     * Returns the number of retained fingerprints.
     *
     * @return retained count
     */
    public synchronized int size() {
        return recent.size();
    }

    /**
     * Returns the total number of fingerprints recorded since startup.
     *
     * @return total recorded failures
     */
    public synchronized long totalCount() {
        return totalCount;
    }

    /**
     * Returns the number of distinct fingerprint ids recorded since startup.
     *
     * @return unique recorded failures
     */
    public synchronized int uniqueCount() {
        return uniqueFingerprintIds.size();
    }

    /**
     * Snapshot of a fingerprint observed by the Spring starter.
     */
    public static final class FingerprintSnapshot {

        private final Instant observedAt;
        private final String id;
        private final String rootCause;
        private final String signature;
        private final int stabilityScore;
        private final String category;
        private final String priority;

        private FingerprintSnapshot(Fingerprint fingerprint) {
            this.observedAt = Instant.now();
            this.id = fingerprint.getId();
            this.rootCause = fingerprint.getRootCause();
            this.signature = fingerprint.getSignature();
            this.stabilityScore = fingerprint.getStabilityScore();
            this.category = fingerprint.getCategory().name();
            this.priority = fingerprint.getPriority().name();
        }

        /**
         * Returns when this fingerprint was recorded.
         *
         * @return observation timestamp
         */
        public Instant getObservedAt() {
            return observedAt;
        }

        /**
         * Returns the fingerprint id.
         *
         * @return fingerprint id
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the root-cause exception.
         *
         * @return root-cause class name
         */
        public String getRootCause() {
            return rootCause;
        }

        /**
         * Returns the origin signature.
         *
         * @return origin signature
         */
        public String getSignature() {
            return signature;
        }

        /**
         * Returns the fingerprint stability score.
         *
         * @return stability score
         */
        public int getStabilityScore() {
            return stabilityScore;
        }

        /**
         * Returns the failure category.
         *
         * @return category name
         */
        public String getCategory() {
            return category;
        }

        /**
         * Returns the failure priority.
         *
         * @return priority name
         */
        public String getPriority() {
            return priority;
        }
    }
}
