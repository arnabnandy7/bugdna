package io.github.bugdna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable occurrence and fingerprint summary for one operational failure family.
 */
public final class FailureFamilyAggregate {

    private final FailureFamily family;
    private final List<FailureAggregate> failures;
    private final long occurrences;

    FailureFamilyAggregate(
            FailureFamily family,
            List<FailureAggregate> failures,
            long occurrences
    ) {
        this.family = Objects.requireNonNull(family, "family must not be null");
        this.failures = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(failures, "failures must not be null")
        ));
        this.occurrences = occurrences;
    }

    /**
     * Returns the operational root-cause family.
     *
     * @return failure family
     */
    public FailureFamily getFamily() {
        return family;
    }

    /**
     * Returns fingerprint aggregates in this family.
     *
     * @return immutable failure aggregate list
     */
    public List<FailureAggregate> getFailures() {
        return failures;
    }

    /**
     * Returns the number of distinct fingerprint IDs in this family.
     *
     * @return unique fingerprint count
     */
    public int getUniqueFailures() {
        return failures.size();
    }

    /**
     * Returns total captured occurrences across the family.
     *
     * @return occurrence count
     */
    public long getOccurrences() {
        return occurrences;
    }
}
