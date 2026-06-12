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

## Available Counts

```java
tracker.getTotalOccurrences();
tracker.getUniqueFailures();
tracker.failures();
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
