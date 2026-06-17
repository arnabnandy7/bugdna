package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        List<Fingerprint> comparisonNewFingerprints = comparison.getNewFingerprints();
        assertThrows(
                UnsupportedOperationException.class,
                comparisonNewFingerprints::clear
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
        assertTrue(drift.isPossibleCodePathChange());
        assertEquals(oldFingerprint, drift.getOldFingerprint());
        assertEquals(newFingerprint, drift.getNewFingerprint());
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
        List<FingerprintDrift> fingerprintDrifts = comparison.getFingerprintDrifts();
        assertThrows(
                UnsupportedOperationException.class,
                fingerprintDrifts::clear
        );
    }

    @Test
    void skipsDriftForRecurringFingerprintWithSameShape() {
        Fingerprint fingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );

        DeploymentComparison comparison = RegressionDetector.compare(
                new DeploymentSnapshot("1.2.0", Collections.singletonList(fingerprint)),
                new DeploymentSnapshot("1.3.0", Collections.singletonList(fingerprint))
        );

        assertEquals(1, comparison.getRecurringFingerprintCount());
        assertEquals(0, comparison.getFingerprintDriftCount());
        assertEquals(Collections.emptyList(), comparison.getFingerprintDrifts());
    }

    @Test
    void compatibilityComparisonConstructorKeepsEmptyDriftList() {
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.NewService",
                "load",
                "com.example.NewService#load"
        );
        Fingerprint resolvedFingerprint = fingerprintShape(
                "BUGDNA-002",
                "com.example.OldService",
                "load",
                "com.example.OldService#load"
        );
        Fingerprint recurringFingerprint = fingerprintShape(
                "BUGDNA-003",
                "com.example.RecurringService",
                "load",
                "com.example.RecurringService#load"
        );

        DeploymentComparison comparison = new DeploymentComparison(
                "1.2.0",
                "1.3.0",
                Collections.singletonList(newFingerprint),
                Collections.singletonList(resolvedFingerprint),
                Collections.singletonList(recurringFingerprint)
        );

        assertEquals(Collections.singletonList(newFingerprint), comparison.getNewFingerprints());
        assertEquals(
                Collections.singletonList(resolvedFingerprint),
                comparison.getResolvedFingerprints()
        );
        assertEquals(
                Collections.singletonList(recurringFingerprint),
                comparison.getRecurringFingerprints()
        );
        assertEquals(0, comparison.getFingerprintDriftCount());
    }

    @Test
    void reportsMinorSignatureDriftBelowCodePathThreshold() {
        Fingerprint oldFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "loadPayment",
                "com.example.PaymentService#loadPayment"
        );
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );

        FingerprintDrift drift = FingerprintDriftDetector.detect(
                oldFingerprint,
                newFingerprint
        );

        assertEquals(14, drift.getSignatureDriftPercentage());
        assertFalse(drift.isPossibleCodePathChange());
        assertEquals(
                "BUGDNA-001"
                        + System.lineSeparator()
                        + "Signature Drift: 14%"
                        + System.lineSeparator()
                        + "Minor signature shape change detected",
                drift.report()
        );
    }

    @Test
    void detectsMinorDriftWhenCurrentMethodExtendsPreviousMethodName() {
        Fingerprint oldFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "loadPayment",
                "com.example.PaymentService#loadPayment"
        );

        FingerprintDrift drift = FingerprintDriftDetector.detect(
                oldFingerprint,
                newFingerprint
        );

        assertEquals(14, drift.getSignatureDriftPercentage());
    }

    @Test
    void detectsDriftWithTokenOverlapAndFrameClassMatch() {
        Fingerprint oldFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                " loadPayment",
                "com.example.PaymentService#loadPayment"
        );
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                " savePayment",
                "com.example.PaymentService#savePayment"
        );

        FingerprintDrift drift = FingerprintDriftDetector.detect(
                oldFingerprint,
                newFingerprint
        );

        assertEquals(27, drift.getSignatureDriftPercentage());
    }

    @Test
    void detectsFullDriftForBlankMethodAndMissingFrameMatches() {
        Fingerprint oldFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "",
                "com.example.PaymentService"
        );
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentGateway",
                "load",
                "com.example.PaymentGateway#load"
        );

        FingerprintDrift drift = FingerprintDriftDetector.detect(
                oldFingerprint,
                newFingerprint
        );

        assertEquals(100, drift.getSignatureDriftPercentage());
        assertTrue(drift.isPossibleCodePathChange());
    }

    @Test
    void detectsFullDriftWhenCurrentMethodIsBlank() {
        Fingerprint oldFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentGateway",
                "",
                "com.example.PaymentGateway"
        );

        FingerprintDrift drift = FingerprintDriftDetector.detect(
                oldFingerprint,
                newFingerprint
        );

        assertEquals(100, drift.getSignatureDriftPercentage());
    }

    @Test
    void detectsFullDriftWhenCurrentMethodHasNoTokens() {
        Fingerprint oldFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentGateway",
                "   ",
                "com.example.PaymentGateway#   "
        );

        FingerprintDrift drift = FingerprintDriftDetector.detect(
                oldFingerprint,
                newFingerprint
        );

        assertEquals(100, drift.getSignatureDriftPercentage());
    }

    @Test
    void detectsFullDriftWhenPreviousOrCurrentFramesAreEmpty() {
        Fingerprint oldFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                Collections.emptyList()
        );
        Fingerprint newFingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );
        Fingerprint oldWithFrame = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );
        Fingerprint newWithoutFrame = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                Collections.emptyList()
        );

        assertEquals(
                46,
                FingerprintDriftDetector.detect(
                        oldFingerprint,
                        newFingerprint
                ).getSignatureDriftPercentage()
        );
        assertEquals(
                46,
                FingerprintDriftDetector.detect(
                        oldWithFrame,
                        newWithoutFrame
                ).getSignatureDriftPercentage()
        );
    }

    @Test
    void driftDetectionRejectsNullFingerprints() {
        Fingerprint fingerprint = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );

        assertThrows(
                NullPointerException.class,
                () -> FingerprintDriftDetector.detect(null, fingerprint)
        );
        assertThrows(
                NullPointerException.class,
                () -> FingerprintDriftDetector.detect(fingerprint, null)
        );
    }

    @Test
    void driftValueRejectsInvalidInput() {
        Fingerprint first = fingerprintShape(
                "BUGDNA-001",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );
        Fingerprint second = fingerprintShape(
                "BUGDNA-002",
                "com.example.PaymentService",
                "load",
                "com.example.PaymentService#load"
        );

        assertThrows(
                NullPointerException.class,
                () -> new FingerprintDrift(null, first, 1)
        );
        assertThrows(
                NullPointerException.class,
                () -> new FingerprintDrift(first, null, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new FingerprintDrift(first, second, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new FingerprintDrift(first, first, -1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new FingerprintDrift(first, first, 101)
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
        List<Fingerprint> fingerprints = snapshot.getFingerprints();
        assertEquals(1, fingerprints.size());
        assertThrows(
                UnsupportedOperationException.class,
                fingerprints::clear
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
        return fingerprintShape(
                id,
                className,
                methodName,
                Collections.singletonList(frame)
        );
    }

    private static Fingerprint fingerprintShape(
            String id,
            String className,
            String methodName,
            List<String> frames
    ) {
        String signature = simpleClassName(className) + "#" + methodName;
        return new Fingerprint(
                id,
                "java.lang.IllegalStateException",
                signature,
                className + "#" + methodName,
                frames,
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
