package io.github.bugdna.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the bugdna Spring Boot starter.
 */
@ConfigurationProperties(prefix = "bugdna")
public class BugDnaProperties {

    private boolean enabled = true;
    private boolean logEnabled = true;
    private boolean mdcEnabled = true;
    private boolean includeStackTrace;
    private int recentLimit = 50;

    /**
     * Creates bugdna starter properties with default values.
     */
    public BugDnaProperties() {
    }

    /**
     * Returns whether bugdna auto-configuration is enabled.
     *
     * @return enabled flag
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether bugdna auto-configuration is enabled.
     *
     * @param enabled enabled flag
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns whether web exceptions should be logged automatically.
     *
     * @return log flag
     */
    public boolean isLogEnabled() {
        return logEnabled;
    }

    /**
     * Sets whether web exceptions should be logged automatically.
     *
     * @param logEnabled log flag
     */
    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }

    /**
     * Returns whether fingerprint fields should be added to SLF4J MDC while logging.
     *
     * @return MDC flag
     */
    public boolean isMdcEnabled() {
        return mdcEnabled;
    }

    /**
     * Sets whether fingerprint fields should be added to SLF4J MDC while logging.
     *
     * @param mdcEnabled MDC flag
     */
    public void setMdcEnabled(boolean mdcEnabled) {
        this.mdcEnabled = mdcEnabled;
    }

    /**
     * Returns whether automatic exception logs include the stack trace.
     *
     * @return stack-trace flag
     */
    public boolean isIncludeStackTrace() {
        return includeStackTrace;
    }

    /**
     * Sets whether automatic exception logs include the stack trace.
     *
     * @param includeStackTrace stack-trace flag
     */
    public void setIncludeStackTrace(boolean includeStackTrace) {
        this.includeStackTrace = includeStackTrace;
    }

    /**
     * Returns the maximum number of recent fingerprints retained for diagnostics.
     *
     * @return recent fingerprint limit
     */
    public int getRecentLimit() {
        return recentLimit;
    }

    /**
     * Sets the maximum number of recent fingerprints retained for diagnostics.
     *
     * @param recentLimit recent fingerprint limit
     */
    public void setRecentLimit(int recentLimit) {
        if (recentLimit < 1) {
            throw new IllegalArgumentException("recentLimit must be at least 1");
        }
        this.recentLimit = recentLimit;
    }
}
