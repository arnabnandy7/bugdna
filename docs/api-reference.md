# API Reference

This page summarizes the public API across **Java** (`io.github.bugdna`), **Node.js / TypeScript** (`bugdna` on npm), and **Python** (`bugdna` on PyPI). Generated Javadocs, TypeScript `.d.ts` declarations, and Python type annotations remain the source for exact method contracts.

## Cross-Language Entry Points

| Capability | Java (`io.github.bugdna`) | Node.js / TypeScript (`bugdna`) | Python (`bugdna`) |
| --- | --- | --- | --- |
| Generate fingerprint | `BugDna.generate(throwable[, context])` | `generate(error[, context])` or `BugDna.generate(...)` | `generate(exc[, context])` or `BugDna.generate(...)` |
| Generate from synthetic frames | *(package-private)* | `generateFromSynthetic(input[, context])` | `generate_from_synthetic(input_data[, context])` |
| Causal dependency graph | `BugDna.dependencyGraph(throwable)` | `dependencyGraph(error)` | `dependency_graph(exc)` |
| PII normalization | `BugDna.normalize(text)` | `normalize(text)` | `normalize(text)` |
| Knowledge base lookup | `BugDna.lookup(id)` | `lookup(id)` | `lookup(id)` |
| Load knowledge base | `BugDna.loadKnowledgeBase(source)` | `loadKnowledgeBase(source)` | `load_knowledge_base(source)` |
| Read knowledge base | `BugDna.readKnowledgeBase(path)` | `readKnowledgeBase(path)` | `read_knowledge_base(path)` |
| Fluent test assertions | `BugDnaAssertions.assertThat(fp)` | `BugDnaAssertions.assertThat(fp)` | `BugDnaAssertions.assert_that(fp)` |
| Compare similarity | `BugSimilarity.compare(fp1, fp2)` | `compareFingerprints(fp1, fp2)` or `BugSimilarity.compare(...)` | `compare_fingerprints(fp1, fp2)` or `BugSimilarity.compare(...)` |
| Diff exceptions / fingerprints | `BugDiff.compare(a, b)` | `diffErrors(e1, e2)` / `diffFingerprints(fp1, fp2)` | `diff_exceptions(e1, e2)` / `diff_fingerprints(fp1, fp2)` |
| Detect signature drift | `FingerprintDriftDetector.detect(oldFp, newFp)` | `detectDrift(oldFp, newFp)` | `detect_drift(old_fp, new_fp)` |
| Deployment regression comparison | `RegressionDetector.compare(oldSnap, newSnap)` | `RegressionDetector.compare(oldSnap, newSnap)` | `RegressionDetector.compare(old_snap, new_snap)` |
| In-memory failure tracker | `new FailureTracker([timelineLimit])` | `new FailureTracker([timelineLimit])` | `FailureTracker([timeline_limit])` |
| Batch skip reason analyzer | `new SkipReasonAnalyzer([tracker])` | `new SkipReasonAnalyzer([tracker])` | `SkipReasonAnalyzer([tracker])` |
| Consumer failure tracker | `new ConsumerFailureTracker()` | `new ConsumerFailureTracker()` | `ConsumerFailureTracker()` |

*(Note: Both Python and TypeScript classes also expose Java-compatible camelCase getter methods such as `getId()`, `getRootCause()`, and `topFailureReport()` alongside native properties/snake_case methods.)*

## Common Recipes

Generate and inspect:

```java
Fingerprint fingerprint = BugDna.generate(failure);
System.out.println(fingerprint.explain());
```

Assert fingerprints in tests:

```java
import static io.github.bugdna.BugDnaAssertions.assertThat;

assertThat(fingerprint)
        .hasCategory(FailureCategory.DATABASE)
        .hasRootCause(SQLTimeoutException.class);
```

Render a causal dependency graph:

```java
FailureDependencyGraph graph = BugDna.dependencyGraph(failure);
System.out.println(graph.report());
```

Normalize high-cardinality message values before custom fingerprint evidence:

```java
BugDna.normalize("Account 123456");     // Account {NUMBER}
BugDna.normalize("john@email.com");     // {EMAIL}
```

Look up runbook context:

```java
FingerprintKnowledge context = BugDna.lookup("BUGDNA-001");
System.out.println(context.getOwner());
System.out.println(context.getRunbook());
```

Generate with impact context:

```java
Fingerprint fingerprint = BugDna.generate(
        failure,
        FailureContext.of(125, 18, false)
);
System.out.println(fingerprint.getPriority()); // HIGH
```

Group recurring failures:

```java
FailureTracker tracker = new FailureTracker();
tracker.capture(failure);
System.out.println(tracker.report());
```

Compare changed failures:

```java
FingerprintDiff diff = BugDiff.compare(previousFailure, currentFailure);
Similarity similarity = BugSimilarity.compare(
        BugDna.generate(previousFailure),
        BugDna.generate(currentFailure)
);
```

## Core Types

### `BugDna`

- `generate(Throwable)` / `generate(failure)`
- `generate(Throwable, FailureContext)` / `generate(failure, context)`
- `dependencyGraph(Throwable)` / `dependency_graph(failure)`
- `normalize(String)`
- `lookup(String)`
- `loadKnowledgeBase(Path | InputStream | Map)` / `load_knowledge_base(...)`
- `readKnowledgeBase(Path)` / `read_knowledge_base(...)`

### `BugDnaAssertions`

- `assertThat(Fingerprint)` / `assert_that(Fingerprint)`

### `BugDnaAssertions.FingerprintAssert` (`FingerprintAssert`)

- `hasCategory(FailureCategory)` / `has_category(...)`
- `hasFamily(FailureFamily)` / `has_family(...)`
- `hasRootCause(Class<? extends Throwable>)`
- `hasRootCause(String)` / `has_root_cause(...)`
- `hasId(String)` / `has_id(...)`
- `hasSignature(String)` / `has_signature(...)`
- `hasQualifiedSignature(String)` / `has_qualified_signature(...)`
- `hasStabilityScore(int)` / `has_stability_score(...)`
- `actual()`

### `FingerprintKnowledge`

- Identity: `getId()`
- Common fields: `getTitle()`, `getOwner()`, `getRunbook()`
- Custom fields: `get(String)`, `getFields()`

### `Fingerprint`

- Identity: `getId()`
- Origin: `getSignature()`, `getQualifiedSignature()`
- Cause: `getRootCause()`, `getCauseChain()`
- Grouping: `getFrames()`, `getFailureChain()`
- Analysis: `getCategory()`, `getFamily()`, `getPriority()`, `getStabilityScore()`
- Explanation: `getExplanation()`, `explain()`

`Fingerprint` equality and hash code are based on the ID.

### `FailureDependencyGraph`

- `getRoot()`
- `getFingerprints()`
- `getDependencies()`
- `getDepth()`
- `report()`

### `FailureContext`

- `unknown()`
- `of(long occurrences, long affectedUsers, boolean fatal)`
- `getOccurrences()`
- `getAffectedUsers()`
- `isFatal()`

### `FailureTracker`

- `capture(Throwable)`
- `capture(Throwable, Instant)`
- `capture(Fingerprint)`
- `capture(Fingerprint, Instant)`
- `timeline()`
- `timelineReport(ZoneId)`
- `bursts(long)`
- `bursts(long, Duration)`
- `burstReport(long, ZoneId)`
- `getTimelineLimit()`
- `failures()`
- `topFailures(int)`
- `families()`
- `topFamilies(int)`
- `getTotalOccurrences()`
- `getUniqueFailures()`
- `getUniqueFamilies()`
- `report()`
- `topFailureReport()`
- `topFailureReport(int)`
- `familyReport()`
- `topFamilyReport()`
- `topFamilyReport(int)`
- `clear()`

### `FailureAggregate`

- `getFingerprint()`
- `getId()`
- `getOccurrences()`

### `FailureOccurrence`

- `getOccurredAt()`
- `getFingerprint()`
- `getId()`

### `FailureBurst`

- `getFingerprint()`
- `getId()`
- `getFirstSeen()`
- `getLastSeen()`
- `getPeakRatePerMinute()`
- `getOccurrences()`
- `getDuration()`
- `report()`

### `DeploymentSnapshot`

- `DeploymentSnapshot(String, Collection<Fingerprint>)`
- `getVersion()`
- `getFingerprints()`

### `RegressionDetector`

- `compare(DeploymentSnapshot, DeploymentSnapshot)`

### `DeploymentComparison`

- `getOldVersion()`
- `getNewVersion()`
- `getNewFingerprints()`
- `getResolvedFingerprints()`
- `getRecurringFingerprints()`
- `getFingerprintDrifts()`
- `getNewFingerprintCount()`
- `getResolvedFingerprintCount()`
- `getRecurringFingerprintCount()`
- `getFingerprintDriftCount()`
- `report()`

### `FingerprintDriftDetector`

- `detect(Fingerprint, Fingerprint)`

### `FingerprintDrift`

- `getId()`
- `getOldFingerprint()`
- `getNewFingerprint()`
- `getSignatureDriftPercentage()`
- `isPossibleCodePathChange()`
- `report()`

### `FailureFamilyAggregate`

- `getFamily()`
- `getFailures()`
- `getUniqueFailures()`
- `getOccurrences()`

### `SkipReasonAnalyzer`

- `record(Throwable)`
- `record(Fingerprint)`
- `getMostCommonFailure()`
- `report()`
- `clear()`

The analyzer is framework-neutral and can be called by a Spring Batch `SkipPolicy`
or any other skip/retry mechanism.

### `ConsumerFailureTracker`

- `capture(String, int, long, Throwable)`
- `capture(String, int, long, Fingerprint)`
- `failures()`
- `report()`
- `clear()`

Consumer failures are grouped by topic and fingerprint.

### `ConsumerFailureAggregate`

- `getTopic()`
- `getPartition()`
- `getOffset()`
- `getFingerprint()`
- `getId()`
- `getOccurrences()`

Partition and offset identify the latest captured occurrence in the aggregate.

### `BugSimilarity`

- `compare(Fingerprint, Fingerprint)`

### `Similarity`

- `getPercentage()`
- `isLikelyRelated()`
- `getExplanation()`

### `BugDiff`

- `compare(Throwable, Throwable)`
- `compare(Fingerprint, Fingerprint)`

### `FingerprintDiff`

- `getSummary()`
- `getOldValue()`
- `getNewValue()`
- `getExplanation()`
- `explain()`

### Enums

- `FailureCategory`
- `FailureFamily`
- `FailurePriority`

## Spring Types

### `@EnableBugDna`

Imports BugDNA core, MVC, WebFlux, and metrics configuration.

### `BugDnaExceptionLogger`

Servlet MVC `HandlerExceptionResolver` that captures and logs failures before
returning control to Spring.

### `BugDnaWebFluxExceptionLogger`

Reactive `WebExceptionHandler` that captures and logs failures before re-emitting
the same error.

### `BugDnaSpringService`

- `fingerprint(Throwable)`
- `fingerprint(Throwable, FailureContext)`
- `diff(Throwable, Throwable)`

`fingerprint(...)` records the result in both the recent repository and shared
`FailureTracker`. `diff(...)` compares failures without recording them.

### `BugDnaProperties`

Bound from the `bugdna` configuration prefix.

### `BugDnaFingerprintRepository`

Stores bounded recent snapshots and lifetime process counters:

- `records(Fingerprint)`
- `recent()`
- `size()`
- `totalCount()`
- `uniqueCount()`

### `BugDnaEndpoint`

Actuator read operation returning recent snapshots.
