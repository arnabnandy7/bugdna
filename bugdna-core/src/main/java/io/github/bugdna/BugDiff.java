package io.github.bugdna;

import java.util.Objects;

/**
 * Explains the most important difference between two failure fingerprints.
 */
public final class BugDiff {

    private BugDiff() {
    }

    /**
     * Generates and compares fingerprints for two failures.
     *
     * @param oldException previous failure
     * @param newException current failure
     * @return fingerprint diff
     * @throws NullPointerException when either failure is {@code null}
     */
    public static FingerprintDiff compare(Throwable oldException, Throwable newException) {
        Objects.requireNonNull(oldException, "oldException must not be null");
        Objects.requireNonNull(newException, "newException must not be null");
        return compare(BugDna.generate(oldException), BugDna.generate(newException));
    }

    /**
     * Compares two existing fingerprints.
     *
     * @param oldFingerprint previous fingerprint
     * @param newFingerprint current fingerprint
     * @return fingerprint diff
     * @throws NullPointerException when either fingerprint is {@code null}
     */
    public static FingerprintDiff compare(
            Fingerprint oldFingerprint,
            Fingerprint newFingerprint
    ) {
        Objects.requireNonNull(oldFingerprint, "oldFingerprint must not be null");
        Objects.requireNonNull(newFingerprint, "newFingerprint must not be null");

        if (oldFingerprint.getId().equals(newFingerprint.getId())) {
            return new FingerprintDiff(
                    "No Fingerprint Change",
                    oldFingerprint.getId(),
                    newFingerprint.getId(),
                    "Both failures resolve to the same fingerprint."
            );
        }

        SignatureParts oldOrigin = SignatureParts.parse(oldFingerprint.getQualifiedSignature());
        SignatureParts newOrigin = SignatureParts.parse(newFingerprint.getQualifiedSignature());
        String oldClassName = simpleClassName(oldOrigin.className);
        String newClassName = simpleClassName(newOrigin.className);
        String oldLayer = detectLayer(oldClassName);
        String newLayer = detectLayer(newClassName);

        if (!oldClassName.equals(newClassName) && oldLayer.equals(newLayer)) {
            return new FingerprintDiff(
                    oldLayer + " Layer Changed",
                    oldClassName,
                    newClassName,
                    "Origin moved within the " + oldLayer + " layer."
            );
        }

        if (!oldLayer.equals(newLayer)) {
            return new FingerprintDiff(
                    "Layer Changed",
                    oldLayer,
                    newLayer,
                    "Origin moved from the " + oldLayer + " layer to the " + newLayer + " layer."
            );
        }

        if (!oldOrigin.methodName.equals(newOrigin.methodName)) {
            return new FingerprintDiff(
                    "Method Changed",
                    oldOrigin.methodName,
                    newOrigin.methodName,
                    "Origin method changed in " + oldClassName + "."
            );
        }

        if (!oldFingerprint.getRootCause().equals(newFingerprint.getRootCause())) {
            return new FingerprintDiff(
                    "Root Cause Changed",
                    simpleClassName(oldFingerprint.getRootCause()),
                    simpleClassName(newFingerprint.getRootCause()),
                    "Root-cause exception type changed."
            );
        }

        return new FingerprintDiff(
                "Call Path Changed",
                oldFingerprint.getSignature(),
                newFingerprint.getSignature(),
                "Fingerprint changed because the normalized call path changed."
        );
    }

    private static String detectLayer(String className) {
        if (className.endsWith("Controller")) {
            return "Controller";
        }
        if (className.endsWith("Service")) {
            return "Service";
        }
        if (className.endsWith("Repository")) {
            return "Repository";
        }
        if (className.endsWith("Gateway")) {
            return "Gateway";
        }
        if (className.endsWith("Client")) {
            return "Client";
        }
        if (className.endsWith("Handler")) {
            return "Handler";
        }
        if (className.endsWith("Validator")) {
            return "Validator";
        }
        if (className.endsWith("Configuration") || className.endsWith("Config")) {
            return "Configuration";
        }
        if (className.endsWith("Mapper")) {
            return "Mapper";
        }
        if (className.endsWith("Codec")) {
            return "Codec";
        }
        return "Application";
    }

    private static String simpleClassName(String className) {
        int packageSeparator = className.lastIndexOf('.');
        String simpleName = packageSeparator >= 0
                ? className.substring(packageSeparator + 1)
                : className;
        return simpleName.replace('$', '.');
    }

    private static final class SignatureParts {

        private final String className;
        private final String methodName;

        private SignatureParts(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        private static SignatureParts parse(String qualifiedSignature) {
            int separator = qualifiedSignature.lastIndexOf('#');
            if (separator < 0) {
                return new SignatureParts(qualifiedSignature, "");
            }
            return new SignatureParts(
                    qualifiedSignature.substring(0, separator),
                    qualifiedSignature.substring(separator + 1)
            );
        }
    }
}
