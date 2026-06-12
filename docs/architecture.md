# Architecture and Data Handling

## Module Boundaries

`bugdna-core` contains deterministic fingerprinting, comparison, and in-memory
aggregation. It targets Java 8 and has no Spring, SLF4J, Micrometer, or persistence
dependency.

`bugdna-spring-boot-starter` targets Java 17 and Spring Boot 4.x. It connects core
features to MVC, WebFlux, logging, MDC, Actuator, and Micrometer.

## Fingerprint Determinism

Fingerprint identity includes:

- Deepest exception type
- Up to five normalized stack frames

Fingerprint identity excludes:

- Exception messages
- Line numbers
- Timestamps
- Request IDs
- User identifiers

This makes equivalent failures stable across varying messages and nearby line edits.
Class or method refactors can intentionally produce a new fingerprint.

## In-Memory State

Two related structures exist in the starter:

- `FailureTracker`: concurrent aggregate counts for all unique IDs seen in process
- `BugDnaFingerprintRepository`: bounded recent snapshots plus lifetime counters

Neither structure persists data. Restarting the process resets all state.

## Concurrency

`FailureTracker` uses `ConcurrentHashMap` and concurrent counters.
`BugDnaFingerprintRepository` uses synchronized operations around its bounded list
and counters.

## Memory Characteristics

The recent repository is bounded by `bugdna.recent-limit`. The tracker retains one
entry per unique fingerprint until `clear()` or process shutdown. Applications with
unbounded dynamically generated stack signatures should monitor unique-count growth.

## Data and Privacy

The fingerprint algorithm does not hash exception messages. It does retain class and
method names in `Fingerprint` objects and recent snapshots.

When stack trace logging is enabled, normal exception messages and stack data are
handled by the logging framework. Review those logs according to application privacy
requirements.

Actuator endpoints should be protected because they expose application class and
method names.

## Failure Handling

The MVC resolver returns `null` after capture. The WebFlux handler re-emits the same
error. Both approaches allow Spring's normal exception resolution to continue.
BugDNA is diagnostic infrastructure, not an error-response framework.
