package io.github.bugdna.build;

/**
 * Severity assigned to a build-time validation issue.
 */
public enum BuildScanSeverity {
    /**
     * Non-failing issue that should be reviewed.
     */
    WARNING,
    /**
     * Failing issue that should block the build by default.
     */
    ERROR
}
