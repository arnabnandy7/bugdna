package io.github.bugdna.build;

/**
 * Build-time validation rules supported by the BugDNA scanner.
 */
public enum BuildScanRule {
    /**
     * Checked-exception API likely called without visible handling.
     */
    UNHANDLED_EXCEPTION,
    /**
     * Catch block that appears to swallow an exception without action.
     */
    EMPTY_CATCH_BLOCK,
    /**
     * Catch or throw site using broad {@code Exception} or {@code Throwable}.
     */
    GENERIC_EXCEPTION_USAGE
}
