package io.github.bugdna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable fingerprint regression comparison between two deployments.
 */
public final class DeploymentComparison {

    private final String oldVersion;
    private final String newVersion;
    private final List<Fingerprint> newFingerprints;
    private final List<Fingerprint> resolvedFingerprints;
    private final List<Fingerprint> recurringFingerprints;
    private final List<FingerprintDrift> fingerprintDrifts;

    DeploymentComparison(
            String oldVersion,
            String newVersion,
            List<Fingerprint> newFingerprints,
            List<Fingerprint> resolvedFingerprints,
            List<Fingerprint> recurringFingerprints
    ) {
        this(
                oldVersion,
                newVersion,
                newFingerprints,
                resolvedFingerprints,
                recurringFingerprints,
                Collections.emptyList()
        );
    }

    DeploymentComparison(
            String oldVersion,
            String newVersion,
            List<Fingerprint> newFingerprints,
            List<Fingerprint> resolvedFingerprints,
            List<Fingerprint> recurringFingerprints,
            List<FingerprintDrift> fingerprintDrifts
    ) {
        this.oldVersion = Objects.requireNonNull(oldVersion, "oldVersion must not be null");
        this.newVersion = Objects.requireNonNull(newVersion, "newVersion must not be null");
        this.newFingerprints = immutableCopy(newFingerprints, "newFingerprints");
        this.resolvedFingerprints = immutableCopy(
                resolvedFingerprints,
                "resolvedFingerprints"
        );
        this.recurringFingerprints = immutableCopy(
                recurringFingerprints,
                "recurringFingerprints"
        );
        this.fingerprintDrifts = immutableDriftCopy(fingerprintDrifts);
    }

    /**
     * Returns the older deployment version.
     *
     * @return old version label
     */
    public String getOldVersion() {
        return oldVersion;
    }

    /**
     * Returns the newer deployment version.
     *
     * @return new version label
     */
    public String getNewVersion() {
        return newVersion;
    }

    /**
     * Returns fingerprints found only in the newer deployment.
     *
     * @return immutable new fingerprint list
     */
    public List<Fingerprint> getNewFingerprints() {
        return newFingerprints;
    }

    /**
     * Returns fingerprints found only in the older deployment.
     *
     * @return immutable resolved fingerprint list
     */
    public List<Fingerprint> getResolvedFingerprints() {
        return resolvedFingerprints;
    }

    /**
     * Returns fingerprints found in both deployments.
     *
     * @return immutable recurring fingerprint list
     */
    public List<Fingerprint> getRecurringFingerprints() {
        return recurringFingerprints;
    }

    /**
     * Returns recurring fingerprints whose signature shape changed.
     *
     * @return immutable fingerprint drift list
     */
    public List<FingerprintDrift> getFingerprintDrifts() {
        return fingerprintDrifts;
    }

    /**
     * Returns the number of new fingerprints.
     *
     * @return new fingerprint count
     */
    public int getNewFingerprintCount() {
        return newFingerprints.size();
    }

    /**
     * Returns the number of resolved fingerprints.
     *
     * @return resolved fingerprint count
     */
    public int getResolvedFingerprintCount() {
        return resolvedFingerprints.size();
    }

    /**
     * Returns the number of recurring fingerprints.
     *
     * @return recurring fingerprint count
     */
    public int getRecurringFingerprintCount() {
        return recurringFingerprints.size();
    }

    /**
     * Returns the number of recurring fingerprints whose signature shape changed.
     *
     * @return drift count
     */
    public int getFingerprintDriftCount() {
        return fingerprintDrifts.size();
    }

    /**
     * Formats the deployment regression summary.
     *
     * @return compact comparison report
     */
    public String report() {
        return "Version " + oldVersion
                + " -> Version " + newVersion
                + System.lineSeparator()
                + System.lineSeparator()
                + "New fingerprints: " + getNewFingerprintCount()
                + System.lineSeparator()
                + "Resolved fingerprints: " + getResolvedFingerprintCount()
                + System.lineSeparator()
                + "Recurring fingerprints: " + getRecurringFingerprintCount()
                + driftReport();
    }

    private static List<Fingerprint> immutableCopy(
            List<Fingerprint> fingerprints,
            String name
    ) {
        return Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(fingerprints, name + " must not be null")
        ));
    }

    private static List<FingerprintDrift> immutableDriftCopy(
            List<FingerprintDrift> drifts
    ) {
        return Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(drifts, "fingerprintDrifts must not be null")
        ));
    }

    private String driftReport() {
        if (fingerprintDrifts.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder()
                .append(System.lineSeparator())
                .append("Fingerprint drifts: ")
                .append(getFingerprintDriftCount());
        for (FingerprintDrift drift : fingerprintDrifts) {
            result.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append(drift.report());
        }
        return result.toString();
    }
}
