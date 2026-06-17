# Changelog

Notable project changes are documented here.

## Unreleased

- No unreleased changes yet.

## 1.1.1 - 2026-06-18

### Added

- Core in-memory `FailureTracker`
- PII-safe fingerprint normalization for email addresses and numeric identifiers
- Immutable `FailureAggregate` snapshots
- Root-cause family classification and cross-fingerprint family aggregation
- Bounded failure timelines with per-minute burst detection
- Deployment regression comparison for new, resolved, and recurring fingerprints
- Fingerprint drift detection for recurring IDs whose signature shape changes
- Top failure reports with configurable limits
- Skip-reason analysis for batch `SkipPolicy` integrations
- Topic, partition, and offset-aware consumer failure tracking
- Spring-managed tracker integration
- `@EnableBugDna` explicit enablement annotation
- Automatic Spring MVC exception fingerprint logging
- Automatic Spring WebFlux exception fingerprint logging
- MDC fingerprint fields
- OpenTelemetry span enrichment with BugDNA fingerprint attributes
- Actuator recent-fingerprint endpoint
- Micrometer total and unique failure metrics
- Structured documentation under `docs/`
- Core-versus-starter feature and integration comparison
- `bugdna analyze <log-file>` command-line failure summary
- `bugdna compare <old-log-file> <new-log-file>` signature comparison

### Changed

- Automatic Spring logs use a compact `[BUGDNA-*]` prefix
- Spring Boot auto-configuration uses `AutoConfiguration.imports`
- Pinned OpenTelemetry Java dependencies to `1.62.0`
- Overrode Maven plugin `plexus-utils` dependency to patched `4.0.3`

### Security

- Addressed OpenTelemetry Java SDK baggage propagation advisory by pinning to `1.62.0`
- Addressed `plexus-utils` directory traversal advisory by using `4.0.3`

## 1.0.1

- Current published project version

Release-specific historical notes before this documentation baseline are available
from the repository's Git history and release pages.
