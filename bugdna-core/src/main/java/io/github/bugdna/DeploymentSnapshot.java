package io.github.bugdna;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable unique fingerprint snapshot for one deployed version.
 */
public final class DeploymentSnapshot {

    private final String version;
    private final List<Fingerprint> fingerprints;

    /**
     * Creates a deployment snapshot.
     *
     * @param version deployed version label
     * @param fingerprints fingerprints observed in the deployment
     */
    public DeploymentSnapshot(String version, Collection<Fingerprint> fingerprints) {
        this.version = validateVersion(version);
        Objects.requireNonNull(fingerprints, "fingerprints must not be null");

        Map<String, Fingerprint> unique = new LinkedHashMap<>();
        for (Fingerprint fingerprint : fingerprints) {
            Fingerprint requiredFingerprint = Objects.requireNonNull(
                    fingerprint,
                    "fingerprints must not contain null"
            );
            unique.put(requiredFingerprint.getId(), requiredFingerprint);
        }
        List<Fingerprint> sorted = new ArrayList<>(unique.values());
        sorted.sort(Comparator.comparing(Fingerprint::getId));
        this.fingerprints = Collections.unmodifiableList(sorted);
    }

    /**
     * Returns the deployed version label.
     *
     * @return version label
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns unique fingerprints sorted by ID.
     *
     * @return immutable fingerprint list
     */
    public List<Fingerprint> getFingerprints() {
        return fingerprints;
    }

    private static String validateVersion(String version) {
        version = Objects.requireNonNull(version, "version must not be null").trim();
        if (version.isEmpty()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        return version;
    }
}
