package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        verifyComparisonCounts(comparison);
        verifyComparisonReport(comparison);
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

        verifyDeduplicatedSnapshot(snapshot);
        verifyInvalidSnapshotRejected(duplicates);
        verifyNullComparisonsRejected(snapshot);
    }

    @Test
    void detectsSignatureDriftForRecurringFingerprintIds() {
        Fingerprint oldFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentGateway",
                "load",
                "com.example.PaymentGateway#load"
        );

        DeploymentComparison comparison = RegressionDetector.compare(
                new DeploymentSnapshot("1.2.0", Collections.singletonList(oldFingerprint)),
                new DeploymentSnapshot("1.3.0", Collections.singletonList(newFingerprint))
        );

        assertEquals(1, comparison.getRecurringFingerprintCount());
        assertEquals(1, comparison.getFingerprintDriftCount());
        FingerprintDrift drift = comparison.getFingerprintDrifts().get(0);
        assertEquals("BUGDNA-001", drift.getId());
        assertEquals(73, drift.getSignatureDriftPercentage());
        assertEquals(true, drift.isPossibleCodePathChange());
        assertEquals(
                "BUGDNA-001"
                        + System.lineSeparator()
                        + "Signature Drift: 73%"
                        + System.lineSeparator()
                        + "Possible code path change detected",
                drift.report()
        );
        assertEquals(
                "Version 1.2.0 -> Version 1.3.0"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "New fingerprints: 0"
                        + System.lineSeparator()
                        + "Resolved fingerprints: 0"
                        + System.lineSeparator()
                        + "Recurring fingerprints: 1"
                        + System.lineSeparator()
                        + "Fingerprint drifts: 1"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "BUGDNA-001"
                        + System.lineSeparator()
                        + "Signature Drift: 73%"
                        + System.lineSeparator()
                        + "Possible code path change detected",
                comparison.report()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> comparison.getFingerprintDrifts().clear()
        );
    }

    @Test
    void driftDetectionRejectsDifferentFingerprintIds() {
        Fingerprint first = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );
        Fingerprint second = fingerprintShape(
                "BUGDNA-002",
                "com.example.PaymentGateway",
                "load",
                "com.example.PaymentGateway#load"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> FingerprintDriftDetector.detect(first, second)
        );
    }

    private static void verifyComparisonCounts(DeploymentComparison comparison) {
        assertEquals("1.2.0", comparison.getOldVersion());
        assertEquals("1.3.0", comparison.getNewVersion());
        assertEquals(4, comparison.getNewFingerprintCount());
        assertEquals(12, comparison.getResolvedFingerprintCount());
        assertEquals(8, comparison.getRecurringFingerprintCount());
    }

    private static void verifyComparisonReport(DeploymentComparison comparison) {
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
    }

    private static void verifyDeduplicatedSnapshot(DeploymentSnapshot snapshot) {
        assertEquals("1.2.0", snapshot.getVersion());
        assertEquals(1, snapshot.getFingerprints().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getFingerprints().clear()
        );
    }

    private static void verifyInvalidSnapshotRejected(List<Fingerprint> duplicates) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeploymentSnapshot(" ", duplicates)
        );
    }

    private static void verifyNullComparisonsRejected(DeploymentSnapshot snapshot) {
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

    private static Fingerprint fingerprintShape(
            String id,
            String className,
            String methodName,
            String frame
    ) {
        String signature = simpleClassName(className) + "#" + methodName;
        return new Fingerprint(
                id,
                "java.lang.IllegalStateException",
                signature,
                className + "#" + methodName,
                Collections.singletonList(frame),
                Collections.singletonList(simpleClassName(className)),
                Arrays.asList("java.lang.IllegalStateException"),
                "Synthetic fingerprint for drift detection tests.",
                90,
                FailurePriority.UNKNOWN,
                FailureCategory.UNKNOWN,
                FailureFamily.UNKNOWN
        );
    }

    private static String simpleClassName(String className) {
        int packageSeparator = className.lastIndexOf('.');
        return packageSeparator >= 0
                ? className.substring(packageSeparator + 1)
                : className;
    }
}
