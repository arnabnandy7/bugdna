package io.github.bugdna;

import java.util.Objects;

/**
 * Describes a same-ID fingerprint whose observed signature shape changed.
 */
public final class FingerprintDrift {

    private static final int CODE_PATH_CHANGE_THRESHOLD = 50;

    private final String id;
    private final Fingerprint oldFingerprint;
    private final Fingerprint newFingerprint;
    private final int signatureDriftPercentage;

    FingerprintDrift(
            Fingerprint oldFingerprint,
            Fingerprint newFingerprint,
            int signatureDriftPercentage
    ) {
        this.oldFingerprint = Objects.requireNonNull(
                oldFingerprint,
                "oldFingerprint must not be null"
        );
        this.newFingerprint = Objects.requireNonNull(
                newFingerprint,
                "newFingerprint must not be null"
        );
        if (!oldFingerprint.getId().equals(newFingerprint.getId())) {
            throw new IllegalArgumentException("drift requires matching fingerprint ids");
        }
        if (signatureDriftPercentage < 0 || signatureDriftPercentage > 100) {
            throw new IllegalArgumentException(
                    "signatureDriftPercentage must be between 0 and 100"
            );
        }
        this.id = oldFingerprint.getId();
        this.signatureDriftPercentage = signatureDriftPercentage;
    }

    /**
     * Returns the drifting fingerprint ID.
     *
     * @return fingerprint ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the previous fingerprint shape.
     *
     * @return old fingerprint
     */
    public Fingerprint getOldFingerprint() {
        return oldFingerprint;
    }

    /**
     * Returns the current fingerprint shape.
     *
     * @return new fingerprint
     */
    public Fingerprint getNewFingerprint() {
        return newFingerprint;
    }

    /**
     * Returns how much the signature and call path changed.
     *
     * @return drift percentage from {@code 0} to {@code 100}
     */
    public int getSignatureDriftPercentage() {
        return signatureDriftPercentage;
    }

    /**
     * Returns whether the drift is large enough to suggest a code path change.
     *
     * @return {@code true} when drift is at least 50%
     */
    public boolean isPossibleCodePathChange() {
        return signatureDriftPercentage >= CODE_PATH_CHANGE_THRESHOLD;
    }

    /**
     * Formats the drift finding for logs and deployment reports.
     *
     * @return compact drift report
     */
    public String report() {
        String message = isPossibleCodePathChange()
                ? "Possible code path change detected"
                : "Minor signature shape change detected";
        return id
                + System.lineSeparator()
                + "Signature Drift: "
                + signatureDriftPercentage
                + "%"
                + System.lineSeparator()
                + message;
    }
}
