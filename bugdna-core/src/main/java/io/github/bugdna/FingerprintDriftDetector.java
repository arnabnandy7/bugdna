package io.github.bugdna;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Detects shape changes for a recurring fingerprint ID.
 */
public final class FingerprintDriftDetector {

    private static final int ORIGIN_CLASS_WEIGHT = 27;
    private static final int METHOD_WEIGHT = 27;
    private static final int FRAME_WEIGHT = 46;

    private FingerprintDriftDetector() {
    }

    /**
     * Compares two observations of the same fingerprint ID for signature drift.
     *
     * @param oldFingerprint previous observation
     * @param newFingerprint current observation
     * @return drift result
     * @throws NullPointerException when either fingerprint is {@code null}
     * @throws IllegalArgumentException when fingerprint IDs differ
     */
    public static FingerprintDrift detect(
            Fingerprint oldFingerprint,
            Fingerprint newFingerprint
    ) {
        oldFingerprint = Objects.requireNonNull(
                oldFingerprint,
                "oldFingerprint must not be null"
        );
        newFingerprint = Objects.requireNonNull(
                newFingerprint,
                "newFingerprint must not be null"
        );
        if (!oldFingerprint.getId().equals(newFingerprint.getId())) {
            throw new IllegalArgumentException("fingerprint IDs must match");
        }

        int similarity = signatureShapeSimilarity(oldFingerprint, newFingerprint);
        return new FingerprintDrift(oldFingerprint, newFingerprint, 100 - similarity);
    }

    static boolean hasSignatureDrift(
            Fingerprint oldFingerprint,
            Fingerprint newFingerprint
    ) {
        return detect(oldFingerprint, newFingerprint).getSignatureDriftPercentage() > 0;
    }

    private static int signatureShapeSimilarity(
            Fingerprint oldFingerprint,
            Fingerprint newFingerprint
    ) {
        SignatureParts oldSignature = SignatureParts.parse(
                oldFingerprint.getQualifiedSignature()
        );
        SignatureParts newSignature = SignatureParts.parse(
                newFingerprint.getQualifiedSignature()
        );
        int classScore = oldSignature.className.equals(newSignature.className)
                ? ORIGIN_CLASS_WEIGHT
                : 0;
        int methodScore = (int) Math.round(
                METHOD_WEIGHT
                        * tokenSimilarity(oldSignature.methodName, newSignature.methodName)
        );
        int frameScore = (int) Math.round(
                FRAME_WEIGHT
                        * frameSimilarity(oldFingerprint.getFrames(), newFingerprint.getFrames())
        );
        return classScore + methodScore + frameScore;
    }

    private static double tokenSimilarity(String first, String second) {
        if (first.equals(second)) {
            return 1.0d;
        }
        if (first.isEmpty() || second.isEmpty()) {
            return 0.0d;
        }
        if (first.startsWith(second) || second.startsWith(first)) {
            return 0.67d;
        }

        Set<String> firstTokens = methodTokens(first);
        Set<String> secondTokens = methodTokens(second);
        if (firstTokens.isEmpty() || secondTokens.isEmpty()) {
            return 0.0d;
        }

        Set<String> intersection = new HashSet<>(firstTokens);
        intersection.retainAll(secondTokens);

        Set<String> union = new HashSet<>(firstTokens);
        union.addAll(secondTokens);

        return (double) intersection.size() / (double) union.size();
    }

    private static double frameSimilarity(Iterable<String> first, Iterable<String> second) {
        Set<String> firstFrames = toSet(first);
        Set<String> secondFrames = toSet(second);
        if (firstFrames.isEmpty() || secondFrames.isEmpty()) {
            return 0.0d;
        }

        double total = 0.0d;
        for (String firstFrame : firstFrames) {
            double best = 0.0d;
            for (String secondFrame : secondFrames) {
                best = Math.max(best, singleFrameSimilarity(firstFrame, secondFrame));
            }
            total += best;
        }

        return total / Math.max(firstFrames.size(), secondFrames.size());
    }

    private static double singleFrameSimilarity(String first, String second) {
        if (first.equals(second)) {
            return 1.0d;
        }

        SignatureParts firstFrame = SignatureParts.parse(first);
        SignatureParts secondFrame = SignatureParts.parse(second);
        if (!firstFrame.className.equals(secondFrame.className)) {
            return 0.0d;
        }

        return 0.7d + (0.3d * tokenSimilarity(firstFrame.methodName, secondFrame.methodName));
    }

    private static Set<String> methodTokens(String methodName) {
        String normalized = methodName.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .replace('-', ' ')
                .toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("\\s+");
        Set<String> tokens = new HashSet<>();

        for (String part : parts) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }

        return tokens;
    }

    private static Set<String> toSet(Iterable<String> values) {
        Set<String> result = new HashSet<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    private static final class SignatureParts {

        private final String className;
        private final String methodName;

        private SignatureParts(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        private static SignatureParts parse(String qualifiedSignature) {
            int separator = qualifiedSignature.lastIndexOf('#');
            if (separator < 0) {
                return new SignatureParts(qualifiedSignature, "");
            }
            return new SignatureParts(
                    qualifiedSignature.substring(0, separator),
                    qualifiedSignature.substring(separator + 1)
            );
        }
    }
}
