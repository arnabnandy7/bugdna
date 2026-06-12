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

    DeploymentComparison(
            String oldVersion,
            String newVersion,
            List<Fingerprint> newFingerprints,
            List<Fingerprint> resolvedFingerprints,
            List<Fingerprint> recurringFingerprints
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
                + "Recurring fingerprints: " + getRecurringFingerprintCount();
    }

    private static List<Fingerprint> immutableCopy(
            List<Fingerprint> fingerprints,
            String name
    ) {
        return Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(fingerprints, name + " must not be null")
        ));
    }
}
