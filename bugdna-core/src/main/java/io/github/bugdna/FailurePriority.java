package io.github.bugdna;

/**
 * Impact-based priority assigned to a failure.
 */
public enum FailurePriority {
    /**
     * No impact context was provided.
     */
    UNKNOWN,

    /**
     * Isolated failure with limited known impact.
     */
    LOW,

    /**
     * Repeated failure or one affecting known users.
     */
    MEDIUM,

    /**
     * Frequent failure or one affecting many users.
     */
    HIGH,

    /**
     * Fatal, extremely frequent, or broadly affecting failure.
     */
    CRITICAL
}
