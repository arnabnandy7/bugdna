package io.github.bugdna;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Creates deterministic fingerprints from Java failures.
 */
public final class BugDna {

    private static final String ID_PREFIX = "BUGDNA-";
    private static final int HASH_LENGTH = 16;
    private static final int MAX_FINGERPRINT_FRAMES = 5;
    private static final String KNOWLEDGE_PATH_PROPERTY = "bugdna.knowledge.path";
    private static final String[] DEFAULT_KNOWLEDGE_FILES = {
            "bugdna.yml", "bugdna.yaml", "bugdna-fingerprints.yml", "bugdna-fingerprints.yaml"
    };
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
    private static final String[] DATABASE_CONTEXT_PATTERNS = {
            "java.sql.", ".sql", "database", "jdbc", "datasource", "hikari",
            "connectionpool", "poolbase", "postgres", "mysql", "mariadb", "oracle"
    };
    private static final String[] CONNECTIVITY_PATTERNS = {
            "connectexception", "socketexception", "sockettimeoutexception",
            "sqltransientconnectionexception", "sqlnontransientconnectionexception",
            "sqlrecoverableexception", "sqltimeoutexception", "jdbcconnectionexception",
            "connection refused", "connection reset", "connection timed out", "socket timeout",
            "communications link failure", "unable to acquire connection",
            "could not open connection", "pool exhausted", "connection pool", "timeout"
    };
    private static volatile Map<String, FingerprintKnowledge> knowledgeBase;

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
        failure = Objects.requireNonNull(failure, "failure must not be null");
        context = Objects.requireNonNull(context, "context must not be null");

        Throwable rootCause = findRootCause(failure);
        String rootCauseName = rootCause.getClass().getName();
        String signature = createSignature(rootCause);
        String qualifiedSignature = createQualifiedSignature(rootCause);
        List<String> frames = createFrameSignature(rootCause);
        List<String> failureChain = createFailureChain(frames);
        List<String> causeChain = createCauseChain(failure);
        String canonicalValue = rootCauseName + "|" + join(frames, "|");
        int stabilityScore = calculateStabilityScore(rootCause);
        FailurePriority priority = prioritize(context);
        FailureCategory category = categorize(rootCause);
        FailureFamily family = classifyFamily(failure, rootCause, frames, category);
        String explanation = createExplanation(
                rootCauseName,
                qualifiedSignature,
                frames.size(),
                stabilityScore,
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
                stabilityScore,
                priority,
                category,
                family
        );
    }

    /**
     * Looks up actionable context for a known fingerprint ID.
     *
     * <p>The first lookup lazily loads a simple YAML knowledge base from the
     * {@code bugdna.knowledge.path} system property, or from one of these files
     * in the current working directory: {@code bugdna.yml}, {@code bugdna.yaml},
     * {@code bugdna-fingerprints.yml}, or {@code bugdna-fingerprints.yaml}.</p>
     *
     * @param id fingerprint ID
     * @return fingerprint context, or {@code null} when the ID is unknown
     * @throws NullPointerException when {@code id} is {@code null}
     * @throws UncheckedIOException when a configured knowledge base cannot be read
     * @throws IllegalArgumentException when the knowledge base is invalid
     */
    public static FingerprintKnowledge lookup(String id) {
        Objects.requireNonNull(id, "id must not be null");
        Map<String, FingerprintKnowledge> loaded = knowledgeBase;
        if (loaded == null) {
            loaded = loadDefaultKnowledgeBase();
            knowledgeBase = loaded;
        }
        return loaded.get(id);
    }

    /**
     * Loads fingerprint context from a simple YAML knowledge base.
     *
     * @param path knowledge base path
     * @throws IOException when the file cannot be read
     */
    public static void loadKnowledgeBase(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream input = Files.newInputStream(path)) {
            loadKnowledgeBase(input);
        }
    }

    /**
     * Loads fingerprint context from a simple YAML knowledge base stream.
     *
     * @param input knowledge base stream
     * @throws IOException when the stream cannot be read
     */
    public static void loadKnowledgeBase(InputStream input) throws IOException {
        knowledgeBase = FingerprintKnowledgeBase.read(input);
    }

    /**
     * Replaces the in-memory fingerprint knowledge base.
     *
     * @param entries fingerprint context keyed by ID
     */
    public static void loadKnowledgeBase(Map<String, FingerprintKnowledge> entries) {
        entries = Objects.requireNonNull(entries, "entries must not be null");
        knowledgeBase = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    /**
     * Reads a simple YAML knowledge base without installing it globally.
     *
     * @param path knowledge base path
     * @return immutable fingerprint context map
     * @throws IOException when the file cannot be read
     */
    public static Map<String, FingerprintKnowledge> readKnowledgeBase(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream input = Files.newInputStream(path)) {
            return FingerprintKnowledgeBase.read(input);
        }
    }

    static void clearKnowledgeBaseForTesting() {
        knowledgeBase = null;
    }

    private static Map<String, FingerprintKnowledge> loadDefaultKnowledgeBase() {
        String configuredPath = System.getProperty(KNOWLEDGE_PATH_PROPERTY);
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            return readKnowledgeBaseUnchecked(Paths.get(configuredPath.trim()));
        }

        for (String fileName : DEFAULT_KNOWLEDGE_FILES) {
            Path path = Paths.get(fileName);
            if (Files.isRegularFile(path)) {
                return readKnowledgeBaseUnchecked(path);
            }
        }

        return Collections.emptyMap();
    }

    private static Map<String, FingerprintKnowledge> readKnowledgeBaseUnchecked(Path path) {
        try {
            return readKnowledgeBase(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to read BugDNA knowledge base: " + path,
                    exception
            );
        }
    }

    private static Throwable findRootCause(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(
                new IdentityHashMap<>()
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
        List<String> frames = new ArrayList<>();

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
                new IdentityHashMap<>()
        );
        List<String> causes = new ArrayList<>();
        Throwable current = failure;

        while (current != null && visited.add(current)) {
            causes.add(current.getClass().getName());
            current = current.getCause();
        }

        return causes;
    }

    private static List<String> createFailureChain(List<String> frames) {
        List<String> chain = new ArrayList<>();

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

    private static int calculateStabilityScore(Throwable rootCause) {
        int stackDepth = rootCause.getStackTrace().length;
        if (stackDepth == 0) {
            return 70;
        }

        int normalizedFrameCount = Math.min(stackDepth, MAX_FINGERPRINT_FRAMES);
        int score = 86 + (normalizedFrameCount * 4);
        return Math.min(score, 98);
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

    private static FailureFamily classifyFamily(
            Throwable failure,
            Throwable rootCause,
            List<String> frames,
            FailureCategory category
    ) {
        String evidence = createFamilyEvidence(failure, rootCause, frames);
        boolean connectivity = matchesAny(evidence, CONNECTIVITY_PATTERNS);
        boolean databaseContext = category == FailureCategory.DATABASE
                || matchesAny(evidence, DATABASE_CONTEXT_PATTERNS);

        if (connectivity && databaseContext) {
            return FailureFamily.DATABASE_CONNECTIVITY;
        }
        if (category == FailureCategory.DATABASE) {
            return FailureFamily.DATABASE_OPERATION;
        }
        if (category == FailureCategory.NETWORK) {
            return FailureFamily.NETWORK_CONNECTIVITY;
        }
        if (category == FailureCategory.VALIDATION) {
            return FailureFamily.VALIDATION;
        }
        if (category == FailureCategory.SECURITY) {
            return FailureFamily.SECURITY;
        }
        if (category == FailureCategory.SERIALIZATION) {
            return FailureFamily.SERIALIZATION;
        }
        if (category == FailureCategory.CONFIGURATION) {
            return FailureFamily.CONFIGURATION;
        }
        if (category == FailureCategory.BUSINESS) {
            return FailureFamily.BUSINESS;
        }
        return FailureFamily.UNKNOWN;
    }

    private static String createFamilyEvidence(
            Throwable failure,
            Throwable rootCause,
            List<String> frames
    ) {
        StringBuilder evidence = new StringBuilder(rootCause.getClass().getName());
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = failure;
        while (current != null && visited.add(current)) {
            evidence.append(' ').append(current.getClass().getName());
            if (current.getMessage() != null) {
                evidence.append(' ').append(current.getMessage());
            }
            current = current.getCause();
        }
        for (String frame : frames) {
            evidence.append(' ').append(frame);
        }
        return evidence.toString().toLowerCase(Locale.ROOT);
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
            int stabilityScore,
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

        explanation.append(" Fingerprint stability confidence is ")
                .append(stabilityScore)
                .append("%.");

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
