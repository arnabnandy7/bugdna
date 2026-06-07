package io.github.bugdna;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * Creates deterministic fingerprints from Java failures.
 */
public final class BugDna {

    private static final String ID_PREFIX = "BUGDNA-";
    private static final int HASH_LENGTH = 6;

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
        Objects.requireNonNull(failure, "failure must not be null");

        Throwable rootCause = findRootCause(failure);
        String rootCauseName = rootCause.getClass().getName();
        String signature = createSignature(rootCause);
        String canonicalValue = rootCauseName + "|" + signature;

        return new Fingerprint(
                ID_PREFIX + shortHash(canonicalValue),
                rootCauseName,
                signature
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
}
