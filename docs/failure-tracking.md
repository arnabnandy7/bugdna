# Failure Tracking

`FailureTracker` aggregates recurring fingerprints in process memory.

## Basic Usage

```java
FailureTracker tracker = new FailureTracker();

tracker.capture(exception);
tracker.capture(exception);

System.out.println(tracker.report());
```

Example:

```text
BUGDNA-7A3F21
Occurrences: 2
```

`capture(Throwable)` generates and returns the fingerprint. Existing fingerprints
can be recorded with `capture(Fingerprint)`.

## Top Failure Report

```java
System.out.println(tracker.topFailureReport());
```

```text
Top 10 Failure Signatures
BUGDNA-001 : 1003
BUGDNA-002 : 512
BUGDNA-003 : 201
```

Use a custom limit:

```java
tracker.topFailureReport(5);
List<FailureAggregate> top = tracker.topFailures(5);
```

Results are ordered by occurrence count descending, then fingerprint ID for
deterministic ties.

## Available Counts

```java
tracker.getTotalOccurrences();
tracker.getUniqueFailures();
tracker.failures();
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
