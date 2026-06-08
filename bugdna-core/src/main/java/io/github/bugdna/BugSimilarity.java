package io.github.bugdna;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Compares failure fingerprints to find related failure families.
 */
public final class BugSimilarity {

    private static final int SAME_ID_SCORE = 100;
    private static final int ROOT_CAUSE_WEIGHT = 35;
    private static final int ORIGIN_CLASS_WEIGHT = 25;
    private static final int METHOD_WEIGHT = 20;
    private static final int FRAME_WEIGHT = 15;
    private static final int CAUSE_CHAIN_WEIGHT = 5;

    private BugSimilarity() {
    }

    /**
     * Compares two fingerprints and returns a percentage similarity score.
     *
     * @param first first fingerprint
     * @param second second fingerprint
     * @return similarity result
     * @throws NullPointerException when either fingerprint is {@code null}
     */
    public static Similarity compare(Fingerprint first, Fingerprint second) {
        first = Objects.requireNonNull(first, "first must not be null");
        second = Objects.requireNonNull(second, "second must not be null");

        if (first.getId().equals(second.getId())) {
            return new Similarity(
                    SAME_ID_SCORE,
                    "Fingerprints have the same id and represent the same failure group."
            );
        }

        SignatureParts firstSignature = SignatureParts.parse(first.getQualifiedSignature());
        SignatureParts secondSignature = SignatureParts.parse(second.getQualifiedSignature());

        int rootScore = equalScore(
                first.getRootCause(),
                second.getRootCause(),
                ROOT_CAUSE_WEIGHT
        );
        int classScore = equalScore(
                firstSignature.className,
                secondSignature.className,
                ORIGIN_CLASS_WEIGHT
        );
        int methodScore = (int) Math.round(
                METHOD_WEIGHT * tokenSimilarity(firstSignature.methodName, secondSignature.methodName)
        );
        int frameScore = (int) Math.round(
                FRAME_WEIGHT * frameSimilarity(first.getFrames(), second.getFrames())
        );
        int causeScore = (int) Math.round(
                CAUSE_CHAIN_WEIGHT * overlap(first.getCauseChain(), second.getCauseChain())
        );
        int total = rootScore + classScore + methodScore + frameScore + causeScore;

        return new Similarity(
                total,
                explanation(first, second, total, rootScore, classScore, methodScore, frameScore, causeScore)
        );
    }

    private static int equalScore(String first, String second, int weight) {
        return first.equals(second) ? weight : 0;
    }

    private static double tokenSimilarity(String first, String second) {
        if (first.equals(second)) {
            return 1.0d;
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

        return total / (double) Math.max(firstFrames.size(), secondFrames.size());
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

    private static double overlap(Iterable<String> first, Iterable<String> second) {
        Set<String> firstValues = toSet(first);
        Set<String> secondValues = toSet(second);

        if (firstValues.isEmpty() || secondValues.isEmpty()) {
            return 0.0d;
        }

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

    private static String explanation(
            Fingerprint first,
            Fingerprint second,
            int total,
            int rootScore,
            int classScore,
            int methodScore,
            int frameScore,
            int causeScore
    ) {
        return "Similarity "
                + total
                + "% between "
                + first.getId()
                + " and "
                + second.getId()
                + " from rootCause="
                + rootScore
                + ", originClass="
                + classScore
                + ", method="
                + methodScore
                + ", frames="
                + frameScore
                + ", causeChain="
                + causeScore
                + ".";
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
