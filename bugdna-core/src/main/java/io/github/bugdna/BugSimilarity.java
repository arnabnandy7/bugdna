package io.github.bugdna;

import java.util.Objects;

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

        FingerprintShape.SignatureParts firstSignature = FingerprintShape.parseSignature(
                first.getQualifiedSignature()
        );
        FingerprintShape.SignatureParts secondSignature = FingerprintShape.parseSignature(
                second.getQualifiedSignature()
        );

        int rootScore = FingerprintShape.equalScore(
                first.getRootCause(),
                second.getRootCause(),
                ROOT_CAUSE_WEIGHT
        );
        int classScore = FingerprintShape.equalScore(
                firstSignature.getClassName(),
                secondSignature.getClassName(),
                ORIGIN_CLASS_WEIGHT
        );
        int methodScore = (int) Math.round(
                METHOD_WEIGHT * FingerprintShape.methodSimilarity(
                        firstSignature.getMethodName(),
                        secondSignature.getMethodName()
                )
        );
        int frameScore = (int) Math.round(
                FRAME_WEIGHT * FingerprintShape.frameSimilarity(
                        first.getFrames(),
                        second.getFrames()
                )
        );
        int causeScore = (int) Math.round(
                CAUSE_CHAIN_WEIGHT * FingerprintShape.overlap(
                        first.getCauseChain(),
                        second.getCauseChain()
                )
        );
        int total = rootScore + classScore + methodScore + frameScore + causeScore;

        return new Similarity(
                total,
                explanation(first, second, total, rootScore, classScore, methodScore, frameScore, causeScore)
        );
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

}
