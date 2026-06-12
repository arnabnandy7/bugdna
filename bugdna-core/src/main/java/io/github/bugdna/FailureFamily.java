package io.github.bugdna;

/**
 * Operational root-cause family that can contain multiple failure fingerprints.
 */
public enum FailureFamily {
    /**
     * Database connection, socket, timeout, and connection-pool failures.
     */
    DATABASE_CONNECTIVITY,

    /**
     * Database failures not identified as connectivity problems.
     */
    DATABASE_OPERATION,

    /**
     * General network, DNS, socket, HTTP, and timeout failures.
     */
    NETWORK_CONNECTIVITY,

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
     * Operational family could not be determined confidently.
     */
    UNKNOWN
}
