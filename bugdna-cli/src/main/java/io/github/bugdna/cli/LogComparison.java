package io.github.bugdna.cli;

/**
 * Immutable signature comparison between an older and newer log analysis.
 */
public final class LogComparison {

    private final int newFailureSignatures;
    private final int resolvedFailureSignatures;

    LogComparison(int newFailureSignatures, int resolvedFailureSignatures) {
        this.newFailureSignatures = newFailureSignatures;
        this.resolvedFailureSignatures = resolvedFailureSignatures;
    }

    /**
     * Returns signatures found only in the newer log.
     *
     * @return new signature count
     */
    public int getNewFailureSignatures() {
        return newFailureSignatures;
    }

    /**
     * Returns signatures found only in the older log.
     *
     * @return resolved signature count
     */
    public int getResolvedFailureSignatures() {
        return resolvedFailureSignatures;
    }

    /**
     * Formats the command-line comparison report.
     *
     * @return formatted comparison
     */
    public String report() {
        return "New Failure Signatures:"
                + System.lineSeparator()
                + newFailureSignatures
                + System.lineSeparator()
                + System.lineSeparator()
                + "Resolved:"
                + System.lineSeparator()
                + resolvedFailureSignatures;
    }
}
