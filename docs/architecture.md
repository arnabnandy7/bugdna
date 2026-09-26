# Architecture and Data Handling

## Module Boundaries

- **`bugdna-core` (Java 8+)**: contains deterministic fingerprinting, comparison, knowledge base lookup, and in-memory aggregation. Has no Spring, SLF4J, Micrometer, or persistence dependency.
- **`bugdna-js` (Node.js 18+ / TypeScript)**: native ESM and CommonJS port (`bugdna` on npm) implementing the full core specification using only Node.js built-in modules (`node:crypto`, `node:fs`, `node:path`).
- **`bugdna-python` (Python 3.9+)**: native `PEP 561` typed port (`bugdna` on PyPI) implementing the full core specification using only the Python standard library.
- **`specification/` & `compatibility-tests/`**: canonical language-neutral algorithm specification, JSON schemas, and 65-vector cross-language test suite verifying identical outputs across Java, Node.js, and Python.
- **`bugdna-spring-boot-starter` (Java 17+, Spring Boot 4.x)**: connects Java core features to Spring MVC, WebFlux, SLF4J logging, MDC, OpenTelemetry span enrichment, Actuator, and Micrometer.
- **`bugdna-build-scanner`, `bugdna-maven-plugin`, `bugdna-gradle-plugin` (Java 8+)**: build-time static source scanner for Java exception-handling hazards.
- **`bugdna-cli` (Java 8+)**: standalone log analyzer and comparator for `BUGDNA-*` identifiers emitted by any language runtime.

## Fingerprint Determinism

Fingerprint identity includes:

- Deepest exception type (discovered via `Throwable.getCause()` in Java, `Error.cause` in Node.js, and `__cause__` / unsuppressed `__context__` in Python, with cycle protection)
- Up to five normalized stack frames (`Class#method`)

Fingerprint identity excludes:

- Exception messages
- Line numbers
- Timestamps
- Request IDs
- User identifiers

This makes equivalent failures stable across varying messages and nearby line edits. Class or method refactors can intentionally produce a new fingerprint.

Root-cause family classification is separate from fingerprint identity. It may use PII-normalized exception messages as heuristic evidence for operational symptoms such as connection refusal or pool exhaustion, but those messages are never included in the fingerprint hash.

## In-Memory State

All core runtimes provide `FailureTracker` (plus `SkipReasonAnalyzer` and `ConsumerFailureTracker`) for concurrent aggregate counts and a bounded timestamped occurrence timeline.

In the Java Spring Boot starter, two related structures exist:

- `FailureTracker`: concurrent aggregate counts for all unique IDs seen in process plus a bounded timestamped occurrence timeline
- `BugDnaFingerprintRepository`: bounded recent snapshots plus lifetime counters

Neither structure persists data. Restarting the process resets all state.

## Concurrency

- **Java**: `FailureTracker` uses `ConcurrentHashMap` and `LongAdder` counters; `BugDnaFingerprintRepository` uses synchronized operations around its bounded list and counters.
- **Python**: `FailureTracker` and `ConsumerFailureTracker` use `threading.RLock` around state and a bounded `collections.deque` for timeline events.
- **Node.js / TypeScript**: `FailureTracker` and `ConsumerFailureTracker` operate synchronously on the Node.js event loop and return frozen (`Object.freeze`) snapshots.

## Memory Characteristics

In the Spring starter, the recent repository is bounded by `bugdna.recent-limit`. Across all languages, `FailureTracker` retains one entry per unique fingerprint until `clear()` or process shutdown, plus at most its configured timeline event limit (default `10,000`). Applications with unbounded dynamically generated stack signatures should monitor unique-count growth.

## Data and Privacy

The fingerprint algorithm does not hash exception messages. It does retain class/module and method/function names in `Fingerprint` objects and recent snapshots. When family evidence inspects exception messages, `normalize(...)` masks email addresses (`{EMAIL}`) and numeric identifiers (`{NUMBER}`) first.

When stack trace logging is enabled in Spring Boot, normal exception messages and stack data are handled by the logging framework. Review those logs according to application privacy requirements.

Actuator endpoints should be protected because they expose application class and method names.

## Failure Handling (Spring Boot)

The MVC resolver returns `null` after capture. The WebFlux handler re-emits the same error. Both approaches allow Spring's normal exception resolution to continue. BugDNA is diagnostic infrastructure, not an error-response framework.
