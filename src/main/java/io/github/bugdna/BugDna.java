package io.github.bugdna;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Creates deterministic fingerprints from Java failures.
 */
public final class BugDna {

    private static final String ID_PREFIX = "BUGDNA-";
    private static final int HASH_LENGTH = 16;
    private static final int MAX_FINGERPRINT_FRAMES = 5;
    private static final String[] DATABASE_PATTERNS = {
            "java.sql.", ".sql", "database", "jdbc", "datasource"
    };
    private static final String[] NETWORK_PATTERNS = {
            "java.net.", "socket", "connect", "network", "timeout", "http"
    };
    private static final String[] VALIDATION_PATTERNS = {
            "validation", "constraint", "illegalargument", "parse", "format"
    };
    private static final String[] SECURITY_PATTERNS = {
            "security", "accessdenied", "authentication", "authorization",
            "permission", "certificate", "ssl", "crypto"
    };
    private static final String[] SERIALIZATION_PATTERNS = {
            "serialization", "deserialization", "invalidclass", "invalidobject",
            "notserializable", "objectstream", "streamcorrupted", "json", "xml",
            "mapping", "codec", "decode", "encode"
    };
    private static final String[] CONFIGURATION_PATTERNS = {
            "configuration", "config", "property", "environment", "missingresource"
    };
    private static final String[] BUSINESS_PATTERNS = {
            "business", "domain", "rule", "policy"
    };

    private BugDna() {
    }

    /**
     * Generates a fingerprint from the deepest cause of the supplied failure.
     *
     * <p>Line numbers and exception messages are intentionally excluded so the
     * same failure remains stable across nearby source edits and varying input.</p>
     *
     * @param failure failure to fingerprint
     * @return immutable failure fingerprint
     * @throws NullPointerException when {@code failure} is {@code null}
     */
    public static Fingerprint generate(Throwable failure) {
        return generate(failure, FailureContext.unknown());
    }

    /**
     * Generates a fingerprint and priority from a failure and its impact context.
     *
     * @param failure failure to fingerprint
     * @param context occurrence and impact information used for prioritization
     * @return immutable failure fingerprint
     * @throws NullPointerException when either argument is {@code null}
     */
    public static Fingerprint generate(Throwable failure, FailureContext context) {
        Objects.requireNonNull(failure, "failure must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Throwable rootCause = findRootCause(failure);
        String rootCauseName = rootCause.getClass().getName();
        String signature = createSignature(rootCause);
        String qualifiedSignature = createQualifiedSignature(rootCause);
        List<String> frames = createFrameSignature(rootCause);
        List<String> failureChain = createFailureChain(frames);
        List<String> causeChain = createCauseChain(failure);
        String canonicalValue = rootCauseName + "|" + join(frames, "|");
        FailurePriority priority = prioritize(context);
        FailureCategory category = categorize(rootCause);
        String explanation = createExplanation(
                rootCauseName,
                qualifiedSignature,
                frames.size(),
                causeChain,
                priority,
                context
        );

        return new Fingerprint(
                ID_PREFIX + shortHash(canonicalValue),
                rootCauseName,
                signature,
                qualifiedSignature,
                frames,
                failureChain,
                causeChain,
                explanation,
                priority,
                category
        );
    }

    private static Throwable findRootCause(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(
                new IdentityHashMap<Throwable, Boolean>()
        );
        Throwable current = failure;

        while (current.getCause() != null && visited.add(current)) {
            if (visited.contains(current.getCause())) {
                break;
            }
            current = current.getCause();
        }

        return current;
    }

    private static String createSignature(Throwable rootCause) {
        StackTraceElement[] stackTrace = rootCause.getStackTrace();
        if (stackTrace.length == 0) {
            return rootCause.getClass().getSimpleName();
        }

        StackTraceElement origin = stackTrace[0];
        return simpleClassName(origin.getClassName()) + "#" + origin.getMethodName();
    }

    private static String createQualifiedSignature(Throwable rootCause) {
        StackTraceElement[] stackTrace = rootCause.getStackTrace();
        if (stackTrace.length == 0) {
            return rootCause.getClass().getName();
        }

        StackTraceElement origin = stackTrace[0];
        return origin.getClassName() + "#" + origin.getMethodName();
    }

    private static List<String> createFrameSignature(Throwable rootCause) {
        StackTraceElement[] stackTrace = rootCause.getStackTrace();
        List<String> frames = new ArrayList<String>();

        for (int i = 0; i < stackTrace.length && frames.size() < MAX_FINGERPRINT_FRAMES; i++) {
            StackTraceElement frame = stackTrace[i];
            frames.add(frame.getClassName() + "#" + frame.getMethodName());
        }

        if (frames.isEmpty()) {
            frames.add(rootCause.getClass().getName());
        }

        return frames;
    }

    private static List<String> createCauseChain(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(
                new IdentityHashMap<Throwable, Boolean>()
        );
        List<String> causes = new ArrayList<String>();
        Throwable current = failure;

        while (current != null && visited.add(current)) {
            causes.add(current.getClass().getName());
            current = current.getCause();
        }

        return causes;
    }

    private static List<String> createFailureChain(List<String> frames) {
        List<String> chain = new ArrayList<String>();

        for (int i = frames.size() - 1; i >= 0; i--) {
            SignatureParts parts = SignatureParts.parse(frames.get(i));
            String simpleName = simpleClassName(parts.className);
            if (chain.isEmpty() || !chain.get(chain.size() - 1).equals(simpleName)) {
                chain.add(simpleName);
            }
        }

        return chain;
    }

    private static FailurePriority prioritize(FailureContext context) {
        if (!context.hasImpactData()) {
            return FailurePriority.UNKNOWN;
        }
        if (context.isFatal()
                || context.getAffectedUsers() >= 100
                || context.getOccurrences() >= 1000) {
            return FailurePriority.CRITICAL;
        }
        if (context.getAffectedUsers() >= 10 || context.getOccurrences() >= 100) {
            return FailurePriority.HIGH;
        }
        if (context.getAffectedUsers() > 0 || context.getOccurrences() >= 10) {
            return FailurePriority.MEDIUM;
        }
        return FailurePriority.LOW;
    }

    private static FailureCategory categorize(Throwable rootCause) {
        String lowerName = rootCause.getClass().getName().toLowerCase(Locale.ROOT);

        if (matchesAny(lowerName, DATABASE_PATTERNS)) {
            return FailureCategory.DATABASE;
        }
        if (matchesAny(lowerName, NETWORK_PATTERNS)) {
            return FailureCategory.NETWORK;
        }
        if (matchesAny(lowerName, VALIDATION_PATTERNS)) {
            return FailureCategory.VALIDATION;
        }
        if (matchesAny(lowerName, SECURITY_PATTERNS)) {
            return FailureCategory.SECURITY;
        }
        if (matchesAny(lowerName, SERIALIZATION_PATTERNS)) {
            return FailureCategory.SERIALIZATION;
        }
        if (matchesAny(lowerName, CONFIGURATION_PATTERNS)) {
            return FailureCategory.CONFIGURATION;
        }
        if (matchesAny(lowerName, BUSINESS_PATTERNS)) {
            return FailureCategory.BUSINESS;
        }

        return FailureCategory.UNKNOWN;
    }

    private static boolean matchesAny(String value, String[] patterns) {
        for (String pattern : patterns) {
            if (value.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static String createExplanation(
            String rootCause,
            String qualifiedSignature,
            int frameCount,
            List<String> causeChain,
            FailurePriority priority,
            FailureContext context
    ) {
        StringBuilder explanation = new StringBuilder();
        explanation.append(rootCause)
                .append(" originated at ")
                .append(qualifiedSignature)
                .append(" and was grouped using ")
                .append(frameCount)
                .append(" normalized stack frame");
        if (frameCount != 1) {
            explanation.append('s');
        }
        explanation.append('.');

        if (causeChain.size() > 1) {
            explanation.append(" Cause chain: ")
                    .append(join(causeChain, " -> "))
                    .append('.');
        }

        if (priority == FailurePriority.UNKNOWN) {
            explanation.append(" Priority is unknown because no impact context was supplied.");
        } else {
            explanation.append(" Priority ")
                    .append(priority.name())
                    .append(" is based on ")
                    .append(context.getOccurrences())
                    .append(" occurrence(s), ")
                    .append(context.getAffectedUsers())
                    .append(" affected user(s), and fatal=")
                    .append(context.isFatal())
                    .append('.');
        }

        return explanation.toString();
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(delimiter);
            }
            result.append(value);
        }
        return result.toString();
    }

    private static String simpleClassName(String className) {
        int packageSeparator = className.lastIndexOf('.');
        String simpleName = packageSeparator >= 0
                ? className.substring(packageSeparator + 1)
                : className;
        return simpleName.replace('$', '.');
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(HASH_LENGTH);

            for (int i = 0; result.length() < HASH_LENGTH; i++) {
                result.append(String.format("%02X", hash[i] & 0xff));
            }

            return result.substring(0, HASH_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static final class SignatureParts {

        private final String className;

        private SignatureParts(String className) {
            this.className = className;
        }

        private static SignatureParts parse(String signature) {
            int separator = signature.lastIndexOf('#');
            if (separator < 0) {
                return new SignatureParts(signature);
            }
            return new SignatureParts(signature.substring(0, separator));
        }
    }
}
