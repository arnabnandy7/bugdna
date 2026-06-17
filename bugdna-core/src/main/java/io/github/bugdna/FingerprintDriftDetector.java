package io.github.bugdna;

import java.util.Objects;

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
        Objects.requireNonNull(
                oldFingerprint,
                "oldFingerprint must not be null"
        );
        Objects.requireNonNull(
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
        FingerprintShape.SignatureParts oldSignature = FingerprintShape.parseSignature(
                oldFingerprint.getQualifiedSignature()
        );
        FingerprintShape.SignatureParts newSignature = FingerprintShape.parseSignature(
                newFingerprint.getQualifiedSignature()
        );
        int classScore = oldSignature.getClassName().equals(newSignature.getClassName())
                ? ORIGIN_CLASS_WEIGHT
                : 0;
        int methodScore = (int) Math.round(
                METHOD_WEIGHT
                        * FingerprintShape.methodSimilarity(
                                oldSignature.getMethodName(),
                                newSignature.getMethodName()
                        )
        );
        int frameScore = (int) Math.round(
                FRAME_WEIGHT
                        * FingerprintShape.frameSimilarity(
                                oldFingerprint.getFrames(),
                                newFingerprint.getFrames()
                        )
        );
        return classScore + methodScore + frameScore;
    }
}
