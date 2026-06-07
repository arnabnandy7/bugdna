package io.github.bugdna;

/**
 * Operational impact data used to prioritize a failure.
 */
public final class FailureContext {

    private static final FailureContext UNKNOWN = new FailureContext(-1, -1, false);

    private final long occurrences;
    private final long affectedUsers;
    private final boolean fatal;

    private FailureContext(long occurrences, long affectedUsers, boolean fatal) {
        this.occurrences = occurrences;
        this.affectedUsers = affectedUsers;
        this.fatal = fatal;
    }

    /**
     * Returns a context with no known impact information.
     *
     * @return unknown context
     */
    public static FailureContext unknown() {
        return UNKNOWN;
    }

    /**
     * Creates a context containing operational impact data.
     *
     * @param occurrences number of observed occurrences
     * @param affectedUsers number of distinct affected users
     * @param fatal whether the failure terminated the process or request
     * @return validated failure context
     * @throws IllegalArgumentException when a count is negative
     */
    public static FailureContext of(long occurrences, long affectedUsers, boolean fatal) {
        if (occurrences < 0) {
            throw new IllegalArgumentException("occurrences must not be negative");
        }
        if (affectedUsers < 0) {
            throw new IllegalArgumentException("affectedUsers must not be negative");
        }
        return new FailureContext(occurrences, affectedUsers, fatal);
    }

    /**
     * Returns the number of observed occurrences.
     *
     * @return occurrence count, or {@code -1} when unknown
     */
    public long getOccurrences() {
        return occurrences;
    }

    /**
     * Returns the number of distinct affected users.
     *
     * @return affected-user count, or {@code -1} when unknown
     */
    public long getAffectedUsers() {
        return affectedUsers;
    }

    /**
     * Returns whether the failure was fatal.
     *
     * @return {@code true} for a fatal failure
     */
    public boolean isFatal() {
        return fatal;
    }

    boolean hasImpactData() {
        return occurrences >= 0 && affectedUsers >= 0;
    }
}
