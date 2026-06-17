# Failure Tracking

`FailureTracker` aggregates recurring fingerprints in process memory.

## Basic Usage

```java
FailureTracker tracker = new FailureTracker();

tracker.capture(firstFailure);
tracker.capture(firstFailure);
tracker.capture(secondFailure);

System.out.println(tracker.report());
```

Example:

```text
2 unique failure signatures

BUGDNA-001
Count: 2

BUGDNA-002
Count: 1
```

`capture(Throwable)` generates and returns the fingerprint. Existing fingerprints
can be recorded with `capture(Fingerprint)`.

## PII-Safe Fingerprinting

BugDNA normalizes common high-cardinality values before they are used as
fingerprint evidence. Numeric tokens become `{NUMBER}` and email addresses become
`{EMAIL}`:

```text
Account 123456 -> Account {NUMBER}
Account 654321 -> Account {NUMBER}
john@email.com -> {EMAIL}
alice@email.com -> {EMAIL}
```

This keeps fingerprints stable when failure messages contain account IDs, order
IDs, user IDs, or email addresses.

## Top Failure Report

```java
System.out.println(tracker.topFailureReport());
```

```text
Top 10 Failure Signatures
BUGDNA-001
Count: 1003
BUGDNA-002
Count: 512
BUGDNA-003
Count: 201
```

Use a custom limit:

```java
tracker.topFailureReport(5);
List<FailureAggregate> top = tracker.topFailures(5);
```

Consume structured aggregates when text output is not appropriate:

```java
for (FailureAggregate failure : tracker.topFailures(5)) {
    dashboard.record(
            failure.getId(),
            failure.getOccurrences(),
            failure.getFingerprint().getCategory()
    );
}
```

Results are ordered by occurrence count descending, then fingerprint ID for
deterministic ties.

## Root-Cause Clustering

Different fingerprints can belong to one operational family:

```java
tracker.capture(connectionRefused);
tracker.capture(socketTimeout);
tracker.capture(poolExhausted);

System.out.println(tracker.familyReport());
```

```text
Root Cause Families

Family: DATABASE_CONNECTIVITY
Occurrences: 3
Unique Failures: 3
BUGDNA-001 (1)
BUGDNA-014 (1)
BUGDNA-027 (1)
```

Consume structured family aggregates:

```java
for (FailureFamilyAggregate family : tracker.families()) {
    family.getFamily();
    family.getOccurrences();
    family.getUniqueFailures();
    family.getFailures();
}
```

Use `topFamilies(int)`, `topFamilyReport()`, or `topFamilyReport(int)` for bounded
views. Family clustering is operational metadata; individual fingerprints remain
the stable identity for exact failure signatures.

## Failure Timeline

Every tracker capture records a timestamp. Normal captures use the current instant;
explicit timestamps are available for imported logs or deterministic processing:

```java
tracker.capture(fingerprint, Instant.parse("2026-06-13T09:01:00Z"));
tracker.capture(fingerprint, Instant.parse("2026-06-13T09:02:00Z"));

System.out.println(tracker.timelineReport(ZoneId.of("Asia/Kolkata")));
```

```text
14:31 BUGDNA-001
14:32 BUGDNA-001
```

`timeline()` returns immutable `FailureOccurrence` values in chronological order.
Each occurrence exposes `getOccurredAt()`, `getFingerprint()`, and `getId()`.

Timeline retention is bounded independently from lifetime aggregate counts:

```java
FailureTracker tracker = new FailureTracker(25_000);
```

The default limit is 10,000 events. When full, the oldest retained event is removed.
`getTotalOccurrences()` still reports all captures since construction or `clear()`.

## Burst Detection

Detect fingerprints whose retained timeline reaches a minimum rate:

```java
List<FailureBurst> bursts = tracker.bursts(300);
System.out.println(tracker.burstReport(300, ZoneId.of("UTC")));
```

```text
BUGDNA-001 burst detected

First Seen: 09:01
Peak Rate: 312/min
Duration: 22 min
```

Peak rate is the highest number of occurrences in one UTC minute. By default, a gap
longer than one minute starts a new burst, preventing unrelated incidents from being
reported as one long duration. Use `bursts(long, Duration)` to choose another idle
boundary.

Each `FailureBurst` exposes:

- `getId()` and `getFingerprint()`
- `getFirstSeen()` and `getLastSeen()`
- `getPeakRatePerMinute()`
- `getDuration()`
- `getOccurrences()`

## Fingerprint Drift Detection

Deployment comparisons detect when a recurring fingerprint ID changes signature
shape after a release:

```java
DeploymentComparison comparison = RegressionDetector.compare(
        previousSnapshot,
        currentSnapshot
);

for (FingerprintDrift drift : comparison.getFingerprintDrifts()) {
    System.out.println(drift.report());
}
```

Example:

```text
BUGDNA-001
Signature Drift: 73%
Possible code path change detected
```

Drift is based on the origin class, origin method, and normalized call path. A high
score means the same fingerprint ID is recurring through a meaningfully different
shape, which often points to a release-time code path change.

## Available Counts

```java
tracker.getTotalOccurrences();
tracker.getUniqueFailures();
tracker.getUniqueFamilies();
tracker.failures();
tracker.families();
```

For 500 captured exceptions grouped into three IDs:

```text
getTotalOccurrences() -> 500
getUniqueFailures()   -> 3
failures().size()     -> 3
```

`failures()` returns an immutable snapshot of all aggregates.

## Concurrency

The implementation uses `ConcurrentHashMap` and concurrent counters. Multiple
application threads can call `capture(...)` safely.

Snapshots are point-in-time views. Captures may continue while a report is being
built, so a report is operationally consistent but is not a global transaction.

## Lifecycle

The tracker:

- Stores no data outside the process
- Loses counts when the process restarts
- Grows with the number of unique fingerprint IDs
- Retains only the configured number of recent timeline events
- Resets when `clear()` is called

Choose persistent monitoring or storage when counts must survive restarts.

## Spring Boot

The starter registers the same core `FailureTracker` as a bean:

```java
@Component
class BatchFailureReport {

    private final FailureTracker tracker;

    BatchFailureReport(FailureTracker tracker) {
        this.tracker = tracker;
    }

    void print() {
        System.out.println(tracker.topFailureReport());
    }
}
```

Automatic MVC captures and `BugDnaSpringService.fingerprint(...)` update the shared
tracker.

## Skip Reason Analysis

`SkipReasonAnalyzer` identifies the failure signature responsible for the most
skipped items:

```java
SkipReasonAnalyzer analyzer = new SkipReasonAnalyzer();

for (Throwable skippedFailure : skippedFailures) {
    analyzer.record(skippedFailure);
}

System.out.println(analyzer.report());
```

```text
Most Common Failure

BUGDNA-001

Count:
421
```

Use it from a Spring Batch `SkipPolicy` without adding a Spring Batch dependency to
BugDNA:

```java
class AnalyzingSkipPolicy implements SkipPolicy {
    private final SkipReasonAnalyzer analyzer;

    AnalyzingSkipPolicy(SkipReasonAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public boolean shouldSkip(Throwable failure, long skipCount) {
        boolean skipped = isSkippable(failure, skipCount);
        if (skipped) {
            analyzer.record(failure);
        }
        return skipped;
    }
}
```

`getMostCommonFailure()` returns the structured `FailureAggregate`. `report()`
returns `None` with count `0` before any skips are recorded.

## Consumer Failure Tracking

`ConsumerFailureTracker` captures topic, partition, offset, and fingerprint without
depending on a specific messaging client:

```java
ConsumerFailureTracker tracker = new ConsumerFailureTracker();

try {
    process(record);
} catch (RuntimeException failure) {
    tracker.capture(
            record.topic(),
            record.partition(),
            record.offset(),
            failure
    );
}

System.out.println(tracker.report());
```

```text
BUGDNA-021

Topic:
payment-events

Occurrences:
203
```

Failures are grouped by topic and fingerprint. This keeps the same fingerprint on
`payment-events` and `refund-events` as separate aggregates. Each
`ConsumerFailureAggregate` exposes the latest captured partition and offset:

```java
ConsumerFailureAggregate failure = tracker.failures().get(0);

failure.getTopic();
failure.getPartition();
failure.getOffset();
failure.getFingerprint();
failure.getOccurrences();
```

Use the overload accepting `Fingerprint` when the failure was fingerprinted
earlier:

```java
tracker.capture("payment-events", 2, 9812L, fingerprint);
```
