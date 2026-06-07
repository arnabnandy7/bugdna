package io.github.bugdna;

import java.util.Objects;

/**
 * An immutable identity for a unique failure.
 */
public final class Fingerprint {

    private final String id;
    private final String rootCause;
    private final String signature;

    Fingerprint(String id, String rootCause, String signature) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.rootCause = Objects.requireNonNull(rootCause, "rootCause must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
    }

    /**
     * Returns the stable identifier for this failure.
     *
     * @return identifier prefixed with {@code BUGDNA-}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the fully qualified class name of the deepest exception cause.
     *
     * @return root-cause class name
     */
    public String getRootCause() {
        return rootCause;
    }

    /**
     * Returns the originating class and method for this failure.
     *
     * @return signature in {@code ClassName#methodName} form
     */
    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Fingerprint)) {
            return false;
        }
        Fingerprint that = (Fingerprint) other;
        return id.equals(that.id)
                && rootCause.equals(that.rootCause)
                && signature.equals(that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rootCause, signature);
    }

    @Override
    public String toString() {
        return "Fingerprint{"
                + "id='" + id + '\''
                + ", rootCause='" + rootCause + '\''
                + ", signature='" + signature + '\''
                + '}';
    }
}
