package io.github.bugdna;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BugDnaTest {

    @TempDir
    Path tempDir;
    private Path createdDefaultKnowledgeBase;
    private boolean hadPreviousKnowledgePath;
    private String previousKnowledgePath;

    @BeforeEach
    void rememberKnowledgeBaseProperty() {
        previousKnowledgePath = System.getProperty("bugdna.knowledge.path");
        hadPreviousKnowledgePath = previousKnowledgePath != null;
    }

    @AfterEach
    void clearKnowledgeBase() throws IOException {
        BugDna.clearKnowledgeBaseForTesting();
        if (hadPreviousKnowledgePath) {
            System.setProperty("bugdna.knowledge.path", previousKnowledgePath);
        } else {
            System.clearProperty("bugdna.knowledge.path");
        }
        if (createdDefaultKnowledgeBase != null) {
            Files.deleteIfExists(createdDefaultKnowledgeBase);
            createdDefaultKnowledgeBase = null;
        }
    }

    @Test
    void nearbyLineNumbersProduceTheSameFingerprint() {
        Throwable line57 = failureAt("com.example.UserService", "getUser", 57);
        Throwable line59 = failureAt("com.example.UserService", "getUser", 59);

        Fingerprint first = BugDna.generate(line57);
        Fingerprint second = BugDna.generate(line59);

        assertEquals(first, second);
        verifyNearbyLineFingerprint(first);
    }

    @Test
    void exceptionMessagesDoNotChangeTheFingerprint() {
        Throwable missingName = failureAt(
                new NullPointerException("name was null"),
                "com.example.UserService",
                "getUser",
                57
        );
        Throwable missingEmail = failureAt(
                new NullPointerException("email was null"),
                "com.example.UserService",
                "getUser",
                57
        );

        assertEquals(BugDna.generate(missingName), BugDna.generate(missingEmail));
    }

    @Test
    void normalizesPiiSafeFingerprintEvidence() {
        assertEquals("Account {NUMBER}", BugDna.normalize("Account 123456"));
        assertEquals("Account {NUMBER}", BugDna.normalize("Account 654321"));
        assertEquals("{EMAIL}", BugDna.normalize("john@email.com"));
        assertEquals("{EMAIL}", BugDna.normalize("alice@email.com"));
    }

    @Test
    void normalizesMultiplePiiTokensInOneMessage() {
        assertEquals(
                "Account {NUMBER} for {EMAIL} failed on order {NUMBER}",
                BugDna.normalize("Account 123456 for john@email.com failed on order 98765")
        );
    }

    @Test
    void normalizesPiiBeforeUsingMessageEvidenceForClassification() {
        Fingerprint fingerprint = BugDna.generate(failureAt(
                new SQLTransientConnectionException(
                        "Connection pool exhausted for account 123456 and john@email.com"
                ),
                "com.example.UserRepository",
                "find",
                30
        ));

        assertEquals(FailureFamily.DATABASE_CONNECTIVITY, fingerprint.getFamily());
    }

    @Test
    void rejectsNullPiiNormalizationInput() {
        assertThrows(NullPointerException.class, () -> BugDna.normalize(null));
    }

    @Test
    void differentMethodsProduceDifferentFingerprints() {
        Throwable getUser = failureAt("com.example.UserService", "getUser", 57);
        Throwable saveUser = failureAt("com.example.UserService", "saveUser", 57);

        assertNotEquals(BugDna.generate(getUser), BugDna.generate(saveUser));
    }

    @Test
    void sameSimpleClassNameInDifferentPackagesProducesDifferentFingerprints() {
        Throwable sales = failureAt("com.example.sales.UserService", "getUser", 57);
        Throwable admin = failureAt("com.example.admin.UserService", "getUser", 57);

        assertNotEquals(BugDna.generate(sales), BugDna.generate(admin));
    }

    @Test
    void differentCallPathsInTheSameMethodProduceDifferentFingerprints() {
        Throwable apiCall = failureWithFrames(
                new NullPointerException(),
                frame("com.example.UserService", "getUser", 57),
                frame("com.example.UserController", "show", 20)
        );
        Throwable batchCall = failureWithFrames(
                new NullPointerException(),
                frame("com.example.UserService", "getUser", 57),
                frame("com.example.UserImporter", "importUsers", 80)
        );

        assertNotEquals(BugDna.generate(apiCall), BugDna.generate(batchCall));
    }

    @Test
    void usesTheDeepestCause() {
        Throwable root = failureAt(
                new NullPointerException("missing user"),
                "com.example.UserService",
                "getUser",
                57
        );
        Throwable wrapper = new IllegalStateException("request failed", root);
        wrapper.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.example.UserController", "show", "UserController.java", 20)
        });

        Fingerprint fingerprint = BugDna.generate(wrapper);

        assertEquals("java.lang.NullPointerException", fingerprint.getRootCause());
        assertEquals("UserService#getUser", fingerprint.getSignature());
        assertEquals(
                Arrays.asList(
                        "java.lang.IllegalStateException",
                        "java.lang.NullPointerException"
                ),
                fingerprint.getCauseChain()
        );
        assertTrue(fingerprint.getExplanation().contains("Cause chain:"));
    }

    @Test
    void buildsFailureDependencyGraphFromCausalChain() {
        Throwable database = failureAt(
                new IllegalArgumentException("bad account"),
                "com.example.AccountRepository",
                "find",
                30
        );
        Throwable consumer = failureAt(
                new IllegalStateException("kafka record failed", database),
                "com.example.AccountConsumer",
                "consume",
                20
        );
        Throwable batch = failureAt(
                new RuntimeException("batch step failed", consumer),
                "com.example.AccountStep",
                "write",
                10
        );

        FailureDependencyGraph graph = BugDna.dependencyGraph(batch);
        List<Fingerprint> fingerprints = graph.getFingerprints();

        assertEquals(3, graph.getDepth());
        assertEquals(fingerprints.get(0), graph.getRoot());
        assertEquals(fingerprints.subList(1, 3), graph.getDependencies());
        assertEquals("AccountStep#write", fingerprints.get(0).getSignature());
        assertEquals("AccountConsumer#consume", fingerprints.get(1).getSignature());
        assertEquals("AccountRepository#find", fingerprints.get(2).getSignature());
        assertEquals(
                fingerprints.get(0).getId()
                        + System.lineSeparator()
                        + " └─ "
                        + fingerprints.get(1).getId()
                        + System.lineSeparator()
                        + "      └─ "
                        + fingerprints.get(2).getId(),
                graph.report()
        );
        assertTrue(graph.toString().contains("FailureDependencyGraph"));
    }

    @Test
    void singleFailureDependencyGraphHasNoDependencies() {
        FailureDependencyGraph graph = BugDna.dependencyGraph(
                failureAt("com.example.AccountConsumer", "consume", 20)
        );

        assertEquals(1, graph.getDepth());
        assertEquals(graph.getRoot().getId(), graph.report());
        assertTrue(graph.getDependencies().isEmpty());
    }

    @Test
    void failureDependencyGraphStopsAtCyclicCauseChains() {
        Throwable first = failureAt(new RuntimeException("first"), "example.First", "run", 1);
        Throwable second = failureAt(new IllegalStateException("second"), "example.Second", "run", 2);
        first.initCause(second);
        second.initCause(first);

        FailureDependencyGraph graph = BugDna.dependencyGraph(first);

        assertEquals(2, graph.getDepth());
    }

    @Test
    void failureDependencyGraphCollectionsAreImmutable() {
        FailureDependencyGraph graph = BugDna.dependencyGraph(
                failureAt("com.example.AccountConsumer", "consume", 20)
        );
        List<Fingerprint> fingerprints = graph.getFingerprints();
        List<Fingerprint> dependencies = graph.getDependencies();

        assertThrows(
                UnsupportedOperationException.class,
                fingerprints::clear
        );
        assertThrows(
                UnsupportedOperationException.class,
                dependencies::clear
        );
    }

    @Test
    void rejectsEmptyFailureDependencyGraph() {
        List<Fingerprint> emptyFingerprints = Collections.emptyList();

        assertThrows(
                IllegalArgumentException.class,
                () -> new FailureDependencyGraph(emptyFingerprints)
        );
    }

    @Test
    void handlesFailuresWithNoStackTrace() {
        Throwable failure = new NullPointerException();
        failure.setStackTrace(new StackTraceElement[0]);

        Fingerprint fingerprint = BugDna.generate(failure);

        assertEquals("NullPointerException", fingerprint.getSignature());
        assertEquals("java.lang.NullPointerException", fingerprint.getQualifiedSignature());
        assertEquals(
                Arrays.asList("NullPointerException"),
                fingerprint.getFailureChain()
        );
        assertEquals(70, fingerprint.getStabilityScore());
    }

    @Test
    void handlesCyclicCauseChains() {
        Throwable first = failureAt(new RuntimeException("first"), "example.First", "run", 1);
        Throwable second = failureAt(new IllegalStateException("second"), "example.Second", "run", 2);

        assertSame(first, first.initCause(second));
        assertSame(second, second.initCause(first));

        Fingerprint fingerprint = BugDna.generate(first);

        assertEquals("java.lang.IllegalStateException", fingerprint.getRootCause());
        assertEquals(
                Arrays.asList(
                        "java.lang.RuntimeException",
                        "java.lang.IllegalStateException"
                ),
                fingerprint.getCauseChain()
        );
    }

    @Test
    void wrapperChangesDoNotSplitTheSameRootFailure() {
        Throwable firstRoot = failureAt("com.example.UserService", "getUser", 57);
        Throwable secondRoot = failureAt("com.example.UserService", "getUser", 59);

        Throwable serviceWrapper = new IllegalStateException("service failed", firstRoot);
        Throwable requestWrapper = new RuntimeException("request failed", secondRoot);

        assertEquals(BugDna.generate(serviceWrapper), BugDna.generate(requestWrapper));
    }

    @Test
    void limitsFingerprintToFiveNormalizedFrames() {
        Throwable failure = failureWithFrames(
                new NullPointerException(),
                frame("example.A", "one", 1),
                frame("example.B", "two", 2),
                frame("example.C", "three", 3),
                frame("example.D", "four", 4),
                frame("example.E", "five", 5),
                frame("example.F", "six", 6)
        );

        Fingerprint fingerprint = BugDna.generate(failure);

        assertEquals(5, fingerprint.getFrames().size());
        assertEquals("example.A#one", fingerprint.getFrames().get(0));
        assertEquals("example.E#five", fingerprint.getFrames().get(4));
    }

    @Test
    void exposesSimplifiedFailureChainForLogs() {
        Throwable failure = failureWithFrames(
                new NullPointerException(),
                frame("com.example.Repository", "find", 10),
                frame("com.example.Service", "getUser", 20),
                frame("com.example.Controller", "show", 30)
        );

        Fingerprint fingerprint = BugDna.generate(failure);

        assertEquals(
                Arrays.asList("Controller", "Service", "Repository"),
                fingerprint.getFailureChain()
        );
    }

    @Test
    void explainsFingerprintAsLogFriendlyBlock() {
        Throwable failure = failureWithFrames(
                new NullPointerException(),
                frame("com.example.Repository", "find", 10),
                frame("com.example.Service", "getUser", 20),
                frame("com.example.Controller", "show", 30)
        );

        Fingerprint fingerprint = BugDna.generate(failure);

        assertEquals(
                fingerprint.getId()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Root Cause:"
                        + System.lineSeparator()
                        + "NullPointerException"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Origin:"
                        + System.lineSeparator()
                        + "Repository#find"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Confidence:"
                        + System.lineSeparator()
                        + "98%"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Failure Chain:"
                        + System.lineSeparator()
                        + "Controller -> Service -> Repository",
                fingerprint.explain()
        );
    }

    @Test
    void scoresFingerprintStabilityFromNormalizedStackEvidence() {
        assertEquals(
                90,
                BugDna.generate(failureAt("example.UserService", "get", 1))
                        .getStabilityScore()
        );
        assertEquals(
                94,
                BugDna.generate(
                        failureWithFrames(
                                new NullPointerException(),
                                frame("example.Repository", "find", 1),
                                frame("example.Service", "get", 2)
                        )
                ).getStabilityScore()
        );
        assertEquals(
                98,
                BugDna.generate(
                        failureWithFrames(
                                new NullPointerException(),
                                frame("example.Repository", "find", 1),
                                frame("example.Service", "get", 2),
                                frame("example.Controller", "show", 3)
                        )
                ).getStabilityScore()
        );
    }

    @Test
    void returnsUnknownPriorityWithoutImpactContext() {
        Fingerprint fingerprint = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 57)
        );

        assertEquals(FailurePriority.UNKNOWN, fingerprint.getPriority());
        assertTrue(fingerprint.getExplanation().contains("Priority is unknown"));
    }

    @Test
    void classifiesSqlTimeoutAsDatabaseFailure() {
        Fingerprint fingerprint = BugDna.generate(
                failureAt(new SQLTimeoutException(), "com.example.UserRepository", "find", 10)
        );

        assertEquals(FailureCategory.DATABASE, fingerprint.getCategory());
        assertEquals(FailureFamily.DATABASE_CONNECTIVITY, fingerprint.getFamily());
    }

    @Test
    void classifiesDatabaseConnectivityAcrossDifferentExceptionTypes() {
        Fingerprint refused = BugDna.generate(failureAt(
                new ConnectException("Connection refused"),
                "org.postgresql.core.PGStream",
                "createSocket",
                10
        ));
        Fingerprint timedOut = BugDna.generate(failureAt(
                new SocketTimeoutException("Read timed out"),
                "com.zaxxer.hikari.pool.PoolBase",
                "newConnection",
                20
        ));
        Fingerprint exhausted = BugDna.generate(failureAt(
                new SQLTransientConnectionException("Connection pool exhausted"),
                "com.example.UserRepository",
                "find",
                30
        ));

        assertNotEquals(refused, timedOut);
        assertNotEquals(timedOut, exhausted);
        assertEquals(FailureFamily.DATABASE_CONNECTIVITY, refused.getFamily());
        assertEquals(FailureFamily.DATABASE_CONNECTIVITY, timedOut.getFamily());
        assertEquals(FailureFamily.DATABASE_CONNECTIVITY, exhausted.getFamily());
    }

    @Test
    void separatesDatabaseOperationsFromConnectivityFailures() {
        Fingerprint fingerprint = BugDna.generate(failureAt(
                new SQLException("syntax error"),
                "com.example.UserRepository",
                "find",
                10
        ));

        assertEquals(FailureFamily.DATABASE_OPERATION, fingerprint.getFamily());
    }

    @Test
    void classifiesCommonExceptionFamilies() {
        verifyCategory(
                FailureCategory.NETWORK,
                BugDna.generate(failureAt(new ConnectException(), "example.Client", "call", 1))
        );
        verifyCategory(
                FailureCategory.VALIDATION,
                BugDna.generate(
                        failureAt(new IllegalArgumentException(), "example.Validator", "check", 1)
                )
        );
        verifyCategory(
                FailureCategory.SECURITY,
                BugDna.generate(
                        failureAt(new AccessDeniedException(), "example.Auth", "check", 1)
                )
        );
        verifyCategory(
                FailureCategory.SERIALIZATION,
                BugDna.generate(
                        failureAt(new InvalidClassException("User"), "example.JsonCodec", "read", 1)
                )
        );
        verifyCategory(
                FailureCategory.CONFIGURATION,
                BugDna.generate(
                        failureAt(
                                new MissingResourceException("missing", "Config", "db.url"),
                                "example.ConfigLoader",
                                "load",
                                1
                        )
                )
        );
        verifyCategory(
                FailureCategory.BUSINESS,
                BugDna.generate(
                        failureAt(new BusinessRuleException(), "example.OrderService", "place", 1)
                )
        );
        verifyCategory(
                FailureCategory.DATABASE,
                BugDna.generate(
                        failureAt(new JdbcDriverException(), "example.Repository", "find", 1)
                )
        );
        verifyCategory(
                FailureCategory.NETWORK,
                BugDna.generate(
                        failureAt(new HttpClientException(), "example.Client", "call", 1)
                )
        );
        verifyCategory(
                FailureCategory.CONFIGURATION,
                BugDna.generate(
                        failureAt(new PropertyLoadException(), "example.ConfigLoader", "load", 1)
                )
        );
        verifyCategory(
                FailureCategory.UNKNOWN,
                BugDna.generate(failureAt(new NullPointerException(), "example.UserService", "get", 1))
        );
    }

    @Test
    void prioritizesUsingOperationalImpact() {
        Throwable failure = failureAt("com.example.UserService", "getUser", 57);

        assertEquals(
                FailurePriority.LOW,
                BugDna.generate(failure, FailureContext.of(1, 0, false)).getPriority()
        );
        assertEquals(
                FailurePriority.MEDIUM,
                BugDna.generate(failure, FailureContext.of(10, 1, false)).getPriority()
        );
        assertEquals(
                FailurePriority.HIGH,
                BugDna.generate(failure, FailureContext.of(100, 10, false)).getPriority()
        );
        assertEquals(
                FailurePriority.CRITICAL,
                BugDna.generate(failure, FailureContext.of(1, 0, true)).getPriority()
        );
    }

    @Test
    void priorityDoesNotChangeFailureIdentity() {
        Throwable first = failureAt("com.example.UserService", "getUser", 57);
        Throwable second = failureAt("com.example.UserService", "getUser", 59);

        Fingerprint low = BugDna.generate(first, FailureContext.of(1, 0, false));
        Fingerprint critical = BugDna.generate(second, FailureContext.of(1000, 100, true));

        assertEquals(low.getId(), critical.getId());
        assertEquals(low, critical);
        assertNotEquals(low.getPriority(), critical.getPriority());
    }

    @Test
    void fingerprintCollectionsAreImmutable() {
        Fingerprint fingerprint = BugDna.generate(
                failureAt("com.example.UserService", "getUser", 57)
        );
        List<String> frames = fingerprint.getFrames();
        List<String> causeChain = fingerprint.getCauseChain();
        List<String> failureChain = fingerprint.getFailureChain();

        assertThrows(
                UnsupportedOperationException.class,
                () -> frames.add("example.Other#method")
        );
        assertThrows(
                UnsupportedOperationException.class,
                causeChain::clear
        );
        assertThrows(
                UnsupportedOperationException.class,
                failureChain::clear
        );
    }

    @Test
    void rejectsInvalidImpactContext() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FailureContext.of(-1, 0, false)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> FailureContext.of(0, -1, false)
        );
    }

    @Test
    void rejectsNullFailures() {
        assertThrows(NullPointerException.class, () -> BugDna.generate(null));
        assertThrows(NullPointerException.class, () -> BugDna.dependencyGraph(null));
    }

    @Test
    void looksUpFingerprintKnowledgeFromSimpleYaml() throws IOException {
        loadYamlKnowledgeBase(
                "BUGDNA-001:\n"
                        + "  title: Database Pool Exhaustion\n"
                        + "  owner: Platform Team\n"
                        + "  runbook: runbooks/db-pool.md\n"
        );

        FingerprintKnowledge context = BugDna.lookup("BUGDNA-001");

        assertEquals("BUGDNA-001", context.getId());
        assertEquals("Database Pool Exhaustion", context.getTitle());
        assertEquals("Platform Team", context.getOwner());
        assertEquals("runbooks/db-pool.md", context.getRunbook());
    }

    @Test
    void keepsArbitraryKnowledgeBaseFields() throws IOException {
        loadYamlKnowledgeBase(
                "BUGDNA-001:\n"
                        + "  title: Database Pool Exhaustion\n"
                        + "  severity: critical\n"
                        + "  dashboard: \"https://example.test/dashboards/db\"\n"
        );

        FingerprintKnowledge context = BugDna.lookup("BUGDNA-001");
        Map<String, String> contextFields = context.getFields();

        verifyArbitraryKnowledgeFields(context);
        assertThrows(NullPointerException.class, () -> context.get(null));
        assertThrows(
                UnsupportedOperationException.class,
                () -> contextFields.put("owner", "Platform Team")
        );
    }

    @Test
    void returnsNullForUnknownFingerprintKnowledge() throws IOException {
        loadYamlKnowledgeBase("BUGDNA-001:\n  title: Known\n");

        assertNull(BugDna.lookup("BUGDNA-404"));
    }

    @Test
    void lazilyLoadsKnowledgeBaseFromConfiguredPath() throws IOException {
        Path knowledgeBase = tempDir.resolve("fingerprints.yml");
        Files.write(
                knowledgeBase,
                Arrays.asList(
                        "BUGDNA-001:",
                        "  title: Database Pool Exhaustion",
                        "  owner: Platform Team"
                ),
                StandardCharsets.UTF_8
        );
        System.setProperty("bugdna.knowledge.path", knowledgeBase.toString());
        BugDna.clearKnowledgeBaseForTesting();

        assertEquals(
                "Platform Team",
                BugDna.lookup("BUGDNA-001").getOwner()
        );
    }

    @Test
    void lazilyLoadsKnowledgeBaseFromDefaultFile() throws IOException {
        Path defaultKnowledgeBase = Paths.get("bugdna.yml");
        assumeFalse(Files.exists(defaultKnowledgeBase));
        Files.write(
                defaultKnowledgeBase,
                Arrays.asList("BUGDNA-001:", "  title: Default File"),
                StandardCharsets.UTF_8
        );
        createdDefaultKnowledgeBase = defaultKnowledgeBase;
        BugDna.clearKnowledgeBaseForTesting();

        assertEquals("Default File", BugDna.lookup("BUGDNA-001").getTitle());
    }

    @Test
    void blankConfiguredKnowledgeBasePathFallsBackToDefaults() {
        System.setProperty("bugdna.knowledge.path", "   ");
        BugDna.clearKnowledgeBaseForTesting();

        assertNull(BugDna.lookup("BUGDNA-001"));
    }

    @Test
    void readsKnowledgeBaseFromPathWithoutInstallingIt() throws IOException {
        Path knowledgeBase = tempDir.resolve("fingerprints.yml");
        Files.write(
                knowledgeBase,
                Arrays.asList("BUGDNA-001:", "  title: From Path"),
                StandardCharsets.UTF_8
        );

        Map<String, FingerprintKnowledge> entries = BugDna.readKnowledgeBase(knowledgeBase);

        assertEquals("From Path", entries.get("BUGDNA-001").getTitle());
        assertNull(BugDna.lookup("BUGDNA-001"));
        assertThrows(
                UnsupportedOperationException.class,
                entries::clear
        );
    }

    @Test
    void loadsKnowledgeBaseFromPath() throws IOException {
        Path knowledgeBase = tempDir.resolve("fingerprints.yml");
        Files.write(
                knowledgeBase,
                Arrays.asList("BUGDNA-001:", "  title: Loaded From Path"),
                StandardCharsets.UTF_8
        );

        BugDna.loadKnowledgeBase(knowledgeBase);

        assertEquals("Loaded From Path", BugDna.lookup("BUGDNA-001").getTitle());
    }

    @Test
    void loadsKnowledgeBaseFromMapDefensively() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "Mapped Knowledge");
        Map<String, FingerprintKnowledge> entries = new LinkedHashMap<>();
        entries.put("BUGDNA-001", new FingerprintKnowledge("BUGDNA-001", fields));

        BugDna.loadKnowledgeBase(entries);
        entries.clear();

        assertEquals("Mapped Knowledge", BugDna.lookup("BUGDNA-001").getTitle());
    }

    @Test
    void rejectsNullKnowledgeBaseInputs() {
        assertThrows(NullPointerException.class, () -> BugDna.lookup(null));
        assertThrows(NullPointerException.class, () -> BugDna.loadKnowledgeBase((Path) null));
        assertThrows(
                NullPointerException.class,
                () -> BugDna.loadKnowledgeBase((ByteArrayInputStream) null)
        );
        assertThrows(
                NullPointerException.class,
                () -> BugDna.loadKnowledgeBase((Map<String, FingerprintKnowledge>) null)
        );
        assertThrows(NullPointerException.class, () -> BugDna.readKnowledgeBase(null));
    }

    @Test
    void wrapsConfiguredKnowledgeBaseReadFailures() {
        System.setProperty(
                "bugdna.knowledge.path",
                tempDir.resolve("missing.yml").toString()
        );

        assertThrows(UncheckedIOException.class, () -> BugDna.lookup("BUGDNA-001"));
    }

    @Test
    void parsesMultipleEntriesCommentsQuotesAndEmptyValues() throws IOException {
        loadYamlKnowledgeBase(
                "# known production fingerprints\n"
                        + "\n"
                        + "BUGDNA-001: # database issue\n"
                        + "  title: 'Database # Pool Exhaustion'\n"
                        + "  owner: \"Platform # Team\"\n"
                        + "  runbook: runbooks/db-pool.md # inline comment\n"
                        + "  notes: # intentionally empty\n"
                        + "  ignored-comment-only: # comment leaves an empty value\n"
                        + "  # ignored field comment\n"
                        + "\n"
                        + "BUGDNA-002:\n"
                        + "  title: Cache Miss Storm\n"
        );

        FingerprintKnowledge firstContext = BugDna.lookup("BUGDNA-001");
        verifyParsedKnowledgeFields(firstContext);
        assertEquals("Cache Miss Storm", BugDna.lookup("BUGDNA-002").getTitle());
    }

    @Test
    void rejectsFieldsBeforeFingerprintIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> loadYamlKnowledgeBase("  title: Orphaned\n")
        );
    }

    @Test
    void rejectsInvalidKnowledgeBaseYaml() {
        assertThrows(
                IllegalArgumentException.class,
                () -> loadYamlKnowledgeBase("BUGDNA-001\n  title: Missing colon\n")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> loadYamlKnowledgeBase(":\n  title: Missing id\n")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> loadYamlKnowledgeBase("BUGDNA-001:\n  title\n")
        );
    }

    private static Throwable failureAt(String className, String methodName, int lineNumber) {
        return failureAt(new NullPointerException(), className, methodName, lineNumber);
    }

    private static ByteArrayInputStream yaml(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void loadYamlKnowledgeBase(String value) throws IOException {
        BugDna.loadKnowledgeBase(yaml(value));
    }

    private static void verifyNearbyLineFingerprint(Fingerprint fingerprint) {
        assertEquals("java.lang.NullPointerException", fingerprint.getRootCause());
        assertEquals("UserService#getUser", fingerprint.getSignature());
        assertEquals("com.example.UserService#getUser", fingerprint.getQualifiedSignature());
        assertEquals(90, fingerprint.getStabilityScore());
        assertTrue(fingerprint.getId().matches("BUGDNA-[0-9A-F]{16}"));
    }

    private static void verifyCategory(
            FailureCategory expectedCategory,
            Fingerprint fingerprint
    ) {
        assertEquals(expectedCategory, fingerprint.getCategory());
    }

    private static void verifyArbitraryKnowledgeFields(FingerprintKnowledge context) {
        assertEquals("critical", context.get("severity"));
        assertEquals("https://example.test/dashboards/db", context.get("dashboard"));
        assertNull(context.get("missing"));
        assertTrue(context.toString().contains("BUGDNA-001"));
    }

    private static void verifyParsedKnowledgeFields(FingerprintKnowledge context) {
        assertEquals("Database # Pool Exhaustion", context.getTitle());
        assertEquals("Platform # Team", context.getOwner());
        assertEquals("runbooks/db-pool.md", context.getRunbook());
        assertEquals("", context.get("notes"));
        assertEquals("", context.get("ignored-comment-only"));
    }

    private static Throwable failureAt(
            Throwable failure,
            String className,
            String methodName,
            int lineNumber
    ) {
        failure.setStackTrace(new StackTraceElement[] {frame(className, methodName, lineNumber)});
        return failure;
    }

    private static Throwable failureWithFrames(
            Throwable failure,
            StackTraceElement... frames
    ) {
        failure.setStackTrace(frames);
        return failure;
    }

    private static StackTraceElement frame(
            String className,
            String methodName,
            int lineNumber
    ) {
        return new StackTraceElement(className, methodName, className + ".java", lineNumber);
    }

    private static final class BusinessRuleException extends RuntimeException {
    }

    private static final class JdbcDriverException extends RuntimeException {
    }

    private static final class HttpClientException extends RuntimeException {
    }

    private static final class PropertyLoadException extends RuntimeException {
    }

    private static final class AccessDeniedException extends RuntimeException {
    }
}
