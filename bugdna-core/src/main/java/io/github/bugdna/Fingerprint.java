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
    private final List<String> failureChain;
    private final List<String> causeChain;
    private final String explanation;
    private final int stabilityScore;
    private final FailurePriority priority;
    private final FailureCategory category;
    private final FailureFamily family;

    Fingerprint(
            String id,
            String rootCause,
            String signature,
            String qualifiedSignature,
            List<String> frames,
            List<String> failureChain,
            List<String> causeChain,
            String explanation,
            int stabilityScore,
            FailurePriority priority,
            FailureCategory category,
            FailureFamily family
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.rootCause = Objects.requireNonNull(rootCause, "rootCause must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        this.qualifiedSignature = Objects.requireNonNull(
                qualifiedSignature,
                "qualifiedSignature must not be null"
        );
        this.frames = immutableCopy(frames, "frames");
        this.failureChain = immutableCopy(failureChain, "failureChain");
        this.causeChain = immutableCopy(causeChain, "causeChain");
        this.explanation = Objects.requireNonNull(explanation, "explanation must not be null");
        this.stabilityScore = validateStabilityScore(stabilityScore);
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.family = Objects.requireNonNull(family, "family must not be null");
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
     * Returns a simplified application chain derived from the normalized frames.
     *
     * @return immutable list of simple class names
     */
    public List<String> getFailureChain() {
        return failureChain;
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
     * Returns confidence that this fingerprint will stay stable as stack traces vary.
     *
     * @return percentage from 0 to 100
     */
    public int getStabilityScore() {
        return stabilityScore;
    }

    /**
     * Returns a compact multi-line explanation suitable for logs.
     *
     * @return log-friendly fingerprint explanation
     */
    public String explain() {
        return id
                + System.lineSeparator()
                + System.lineSeparator()
                + "Root Cause:"
                + System.lineSeparator()
                + simpleClassName(rootCause)
                + System.lineSeparator()
                + System.lineSeparator()
                + "Origin:"
                + System.lineSeparator()
                + signature
                + System.lineSeparator()
                + System.lineSeparator()
                + "Confidence:"
                + System.lineSeparator()
                + stabilityScore
                + "%"
                + System.lineSeparator()
                + System.lineSeparator()
                + "Failure Chain:"
                + System.lineSeparator()
                + join(failureChain, " -> ");
    }

    /**
     * Returns the impact-based priority.
     *
     * @return priority, or {@link FailurePriority#UNKNOWN} without impact context
     */
    public FailurePriority getPriority() {
        return priority;
    }

    /**
     * Returns the broad failure family for the root-cause exception.
     *
     * @return classified failure category
     */
    public FailureCategory getCategory() {
        return category;
    }

    /**
     * Returns the operational root-cause family used to cluster related fingerprints.
     *
     * @return classified failure family
     */
    public FailureFamily getFamily() {
        return family;
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
                + ", stabilityScore=" + stabilityScore
                + ", priority=" + priority
                + ", category=" + category
                + ", family=" + family
                + '}';
    }

    private static int validateStabilityScore(int stabilityScore) {
        if (stabilityScore < 0 || stabilityScore > 100) {
            throw new IllegalArgumentException("stabilityScore must be between 0 and 100");
        }
        return stabilityScore;
    }

    private static List<String> immutableCopy(List<String> values, String name) {
        values = Objects.requireNonNull(values, name + " must not be null");
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String simpleClassName(String className) {
        int packageSeparator = className.lastIndexOf('.');
        String simpleName = packageSeparator >= 0
                ? className.substring(packageSeparator + 1)
                : className;
        return simpleName.replace('$', '.');
    }

    private static String join(List<String> values, String delimiter) {
        return String.join(delimiter, values);
    }
}
