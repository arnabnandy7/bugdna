package io.github.bugdna;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compares unique fingerprints observed in two deployments.
 */
public final class RegressionDetector {

    private RegressionDetector() {
    }

    /**
     * Compares an older deployment with a newer deployment.
     *
     * @param oldDeployment older deployment snapshot
     * @param newDeployment newer deployment snapshot
     * @return deployment fingerprint comparison
     */
    public static DeploymentComparison compare(
            DeploymentSnapshot oldDeployment,
            DeploymentSnapshot newDeployment
    ) {
        oldDeployment = Objects.requireNonNull(
                oldDeployment,
                "oldDeployment must not be null"
        );
        newDeployment = Objects.requireNonNull(
                newDeployment,
                "newDeployment must not be null"
        );

        Map<String, Fingerprint> oldFingerprints = byId(oldDeployment);
        Map<String, Fingerprint> newFingerprints = byId(newDeployment);

        List<Fingerprint> added = new ArrayList<>();
        List<Fingerprint> recurring = new ArrayList<>();
        for (Map.Entry<String, Fingerprint> entry : newFingerprints.entrySet()) {
            if (oldFingerprints.containsKey(entry.getKey())) {
                recurring.add(entry.getValue());
            } else {
                added.add(entry.getValue());
            }
        }

        List<Fingerprint> resolved = new ArrayList<>();
        for (Map.Entry<String, Fingerprint> entry : oldFingerprints.entrySet()) {
            if (!newFingerprints.containsKey(entry.getKey())) {
                resolved.add(entry.getValue());
            }
        }

        return new DeploymentComparison(
                oldDeployment.getVersion(),
                newDeployment.getVersion(),
                added,
                resolved,
                recurring
        );
    }

    private static Map<String, Fingerprint> byId(DeploymentSnapshot deployment) {
        Map<String, Fingerprint> fingerprints = new LinkedHashMap<>();
        for (Fingerprint fingerprint : deployment.getFingerprints()) {
            fingerprints.put(fingerprint.getId(), fingerprint);
        }
        return fingerprints;
    }
}
