# API Reference

This page summarizes the public API. Generated Javadocs remain the source for exact
method contracts.

## Core Types

### `BugDna`

- `generate(Throwable)`
- `generate(Throwable, FailureContext)`

### `Fingerprint`

- Identity: `getId()`
- Origin: `getSignature()`, `getQualifiedSignature()`
- Cause: `getRootCause()`, `getCauseChain()`
- Grouping: `getFrames()`, `getFailureChain()`
- Analysis: `getCategory()`, `getPriority()`, `getStabilityScore()`
- Explanation: `getExplanation()`, `explain()`

`Fingerprint` equality and hash code are based on the ID.

### `FailureContext`

- `unknown()`
- `of(long occurrences, long affectedUsers, boolean fatal)`
- `getOccurrences()`
- `getAffectedUsers()`
- `isFatal()`

### `FailureTracker`

- `capture(Throwable)`
- `capture(Fingerprint)`
- `failures()`
- `topFailures(int)`
- `getTotalOccurrences()`
- `getUniqueFailures()`
- `report()`
- `topFailureReport()`
- `topFailureReport(int)`
- `clear()`

### `FailureAggregate`

- `getFingerprint()`
- `getId()`
- `getOccurrences()`

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
- `FailurePriority`

## Spring Types

### `@EnableBugDna`

Imports BugDNA core, web, and metrics configuration.

### `BugDnaSpringService`

- `fingerprint(Throwable)`
- `fingerprint(Throwable, FailureContext)`
- `diff(Throwable, Throwable)`

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
