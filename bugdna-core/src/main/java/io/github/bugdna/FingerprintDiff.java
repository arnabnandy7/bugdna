package io.github.bugdna;

import java.util.Objects;

/**
 * Human-readable difference between an old and new failure fingerprint.
 */
public final class FingerprintDiff {

    private final String summary;
    private final String oldValue;
    private final String newValue;
    private final String explanation;

    FingerprintDiff(
            String summary,
            String oldValue,
            String newValue,
            String explanation
    ) {
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.oldValue = Objects.requireNonNull(oldValue, "oldValue must not be null");
        this.newValue = Objects.requireNonNull(newValue, "newValue must not be null");
        this.explanation = Objects.requireNonNull(explanation, "explanation must not be null");
    }

    /**
     * Returns the headline change.
     *
     * @return short diff summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Returns the previous value for the changed dimension.
     *
     * @return old value
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * Returns the current value for the changed dimension.
     *
     * @return new value
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * Returns a human-readable explanation for the diff.
     *
     * @return diff explanation
     */
    public String getExplanation() {
        return explanation;
    }

    /**
     * Returns a compact multi-line diff suitable for regression logs.
     *
     * @return log-friendly diff
     */
    public String explain() {
        return summary
                + System.lineSeparator()
                + System.lineSeparator()
                + "Old:"
                + System.lineSeparator()
                + oldValue
                + System.lineSeparator()
                + System.lineSeparator()
                + "New:"
                + System.lineSeparator()
                + newValue;
    }

    @Override
    public String toString() {
        return "FingerprintDiff{"
                + "summary='" + summary + '\''
                + ", oldValue='" + oldValue + '\''
                + ", newValue='" + newValue + '\''
                + ", explanation='" + explanation + '\''
                + '}';
    }
}
