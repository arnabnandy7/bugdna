package io.github.bugdna;

import java.util.Objects;

/**
 * Similarity score between two failure fingerprints.
 */
public final class Similarity {

    private final int percentage;
    private final String explanation;

    Similarity(int percentage, String explanation) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        }
        this.percentage = percentage;
        this.explanation = Objects.requireNonNull(explanation, "explanation must not be null");
    }

    /**
     * Returns the similarity score as a percentage.
     *
     * @return score from {@code 0} to {@code 100}
     */
    public int getPercentage() {
        return percentage;
    }

    /**
     * Returns whether the score is high enough to treat failures as related.
     *
     * @return {@code true} when the score is at least {@code 80}
     */
    public boolean isLikelyRelated() {
        return percentage >= 80;
    }

    /**
     * Returns a human-readable explanation for the score.
     *
     * @return similarity explanation
     */
    public String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return "Similarity{"
                + "percentage=" + percentage
                + ", likelyRelated=" + isLikelyRelated()
                + ", explanation='" + explanation + '\''
                + '}';
    }
}
