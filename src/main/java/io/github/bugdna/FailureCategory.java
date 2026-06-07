package io.github.bugdna;

/**
 * Broad family assigned to a failure from its root-cause exception.
 */
public enum FailureCategory {
    /**
     * Database and SQL failures.
     */
    DATABASE,

    /**
     * Network, socket, DNS, and timeout failures.
     */
    NETWORK,

    /**
     * Invalid input, parsing, and constraint failures.
     */
    VALIDATION,

    /**
     * Authentication, authorization, and cryptography failures.
     */
    SECURITY,

    /**
     * Serialization, deserialization, encoding, and decoding failures.
     */
    SERIALIZATION,

    /**
     * Missing or invalid runtime configuration failures.
     */
    CONFIGURATION,

    /**
     * Domain-specific application failures.
     */
    BUSINESS,

    /**
     * Failure family could not be determined confidently.
     */
    UNKNOWN
}
