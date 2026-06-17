package io.github.bugdna;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Shared comparison helpers for fingerprint signature and call-path shape.
 */
final class FingerprintShape {

    private FingerprintShape() {
    }

    static int equalScore(String first, String second, int weight) {
        return first.equals(second) ? weight : 0;
    }

    static double methodSimilarity(String first, String second) {
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

        return overlap(firstTokens, secondTokens);
    }

    static double frameSimilarity(Iterable<String> first, Iterable<String> second) {
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

    static double overlap(Iterable<String> first, Iterable<String> second) {
        Set<String> firstValues = toSet(first);
        Set<String> secondValues = toSet(second);
        if (firstValues.isEmpty() || secondValues.isEmpty()) {
            return 0.0d;
        }

        return overlap(firstValues, secondValues);
    }

    static SignatureParts parseSignature(String qualifiedSignature) {
        int separator = qualifiedSignature.lastIndexOf('#');
        if (separator < 0) {
            return new SignatureParts(qualifiedSignature, "");
        }
        return new SignatureParts(
                qualifiedSignature.substring(0, separator),
                qualifiedSignature.substring(separator + 1)
        );
    }

    private static double singleFrameSimilarity(String first, String second) {
        if (first.equals(second)) {
            return 1.0d;
        }

        SignatureParts firstFrame = parseSignature(first);
        SignatureParts secondFrame = parseSignature(second);
        if (!firstFrame.getClassName().equals(secondFrame.getClassName())) {
            return 0.0d;
        }

        return 0.7d
                + (0.3d * methodSimilarity(
                        firstFrame.getMethodName(),
                        secondFrame.getMethodName()
                ));
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

    private static double overlap(Set<String> firstValues, Set<String> secondValues) {
        Set<String> intersection = new HashSet<>(firstValues);
        intersection.retainAll(secondValues);

        Set<String> union = new HashSet<>(firstValues);
        union.addAll(secondValues);

        return (double) intersection.size() / (double) union.size();
    }

    private static Set<String> toSet(Iterable<String> values) {
        Set<String> result = new HashSet<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    static final class SignatureParts {

        private final String className;
        private final String methodName;

        private SignatureParts(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        String getClassName() {
            return className;
        }

        String getMethodName() {
            return methodName;
        }
    }
}
