# API Reference

This page summarizes the public API. Generated Javadocs remain the source for exact
method contracts.

## Common Recipes

Generate and inspect:

```java
Fingerprint fingerprint = BugDna.generate(failure);
System.out.println(fingerprint.explain());
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

- `generate(Throwable)`
- `generate(Throwable, FailureContext)`
- `dependencyGraph(Throwable)`
- `normalize(String)`
- `lookup(String)`
- `loadKnowledgeBase(Path)`
- `loadKnowledgeBase(InputStream)`
- `loadKnowledgeBase(Map<String, FingerprintKnowledge>)`
- `readKnowledgeBase(Path)`

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
