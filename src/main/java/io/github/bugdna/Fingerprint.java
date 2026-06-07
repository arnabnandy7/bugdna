package io.github.bugdna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An immutable identity for a unique failure.
 */
public final class Fingerprint {

    private final String id;
    private final String rootCause;
    private final String signature;
    private final String qualifiedSignature;
    private final List<String> frames;
    private final List<String> causeChain;
    private final String explanation;
    private final FailurePriority priority;

    Fingerprint(
            String id,
            String rootCause,
            String signature,
            String qualifiedSignature,
            List<String> frames,
            List<String> causeChain,
            String explanation,
            FailurePriority priority
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.rootCause = Objects.requireNonNull(rootCause, "rootCause must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        this.qualifiedSignature = Objects.requireNonNull(
                qualifiedSignature,
                "qualifiedSignature must not be null"
        );
        this.frames = immutableCopy(frames, "frames");
        this.causeChain = immutableCopy(causeChain, "causeChain");
        this.explanation = Objects.requireNonNull(explanation, "explanation must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
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

    /**
     * Returns the fully qualified originating class and method.
     *
     * @return signature in {@code package.ClassName#methodName} form
     */
    public String getQualifiedSignature() {
        return qualifiedSignature;
    }

    /**
     * Returns the normalized stack frames used to group this failure.
     *
     * @return immutable list of fully qualified class and method names
     */
    public List<String> getFrames() {
        return frames;
    }

    /**
     * Returns exception class names from the outer failure to its deepest cause.
     *
     * @return immutable cause-chain list
     */
    public List<String> getCauseChain() {
        return causeChain;
    }

    /**
     * Returns a human-readable description of the grouping and priority.
     *
     * @return failure explanation
     */
    public String getExplanation() {
        return explanation;
    }

    /**
     * Returns the impact-based priority.
     *
     * @return priority, or {@link FailurePriority#UNKNOWN} without impact context
     */
    public FailurePriority getPriority() {
        return priority;
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
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Fingerprint{"
                + "id='" + id + '\''
                + ", rootCause='" + rootCause + '\''
                + ", signature='" + signature + '\''
                + ", priority=" + priority
                + '}';
    }

    private static List<String> immutableCopy(List<String> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }
}
