package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegressionDetectorTest {

    @Test
    void comparesNewResolvedAndRecurringFingerprintsAcrossDeployments() {
        List<Fingerprint> oldFingerprints = fingerprints("old", 1, 12);
        oldFingerprints.addAll(fingerprints("recurring", 1, 8));

        List<Fingerprint> newFingerprints = fingerprints("recurring", 1, 8);
        newFingerprints.addAll(fingerprints("new", 1, 4));

        DeploymentComparison comparison = RegressionDetector.compare(
                new DeploymentSnapshot("1.2.0", oldFingerprints),
                new DeploymentSnapshot("1.3.0", newFingerprints)
        );

        assertEquals("1.2.0", comparison.getOldVersion());
        assertEquals("1.3.0", comparison.getNewVersion());
        assertEquals(4, comparison.getNewFingerprintCount());
        assertEquals(12, comparison.getResolvedFingerprintCount());
        assertEquals(8, comparison.getRecurringFingerprintCount());
        assertEquals(
                "Version 1.2.0 -> Version 1.3.0"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "New fingerprints: 4"
                        + System.lineSeparator()
                        + "Resolved fingerprints: 12"
                        + System.lineSeparator()
                        + "Recurring fingerprints: 8",
                comparison.report()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> comparison.getNewFingerprints().clear()
        );
    }

    @Test
    void snapshotsDeduplicateFingerprintsAndRejectInvalidInput() {
        Fingerprint fingerprint = fingerprint("same", 1);
        List<Fingerprint> duplicates = new ArrayList<>();
        duplicates.add(fingerprint);
        duplicates.add(fingerprint);

        DeploymentSnapshot snapshot = new DeploymentSnapshot(" 1.2.0 ", duplicates);

        assertEquals("1.2.0", snapshot.getVersion());
        assertEquals(1, snapshot.getFingerprints().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getFingerprints().clear()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeploymentSnapshot(" ", duplicates)
        );
        assertThrows(
                NullPointerException.class,
                () -> RegressionDetector.compare(null, snapshot)
        );
        assertThrows(
                NullPointerException.class,
                () -> RegressionDetector.compare(snapshot, null)
        );
    }

    private static List<Fingerprint> fingerprints(
            String prefix,
            int start,
            int count
    ) {
        List<Fingerprint> fingerprints = new ArrayList<>();
        for (int index = start; index < start + count; index++) {
            fingerprints.add(fingerprint(prefix, index));
        }
        return fingerprints;
    }

    private static Fingerprint fingerprint(String prefix, int index) {
        NullPointerException failure = new NullPointerException();
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(
                        "com.example." + prefix + "Service",
                        "operation" + index,
                        prefix + "Service.java",
                        index
                )
        });
        return BugDna.generate(failure);
    }
}
