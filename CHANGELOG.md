# Changelog

Notable project changes are documented here.

## Unreleased

### Added

- Core in-memory `FailureTracker`
- Immutable `FailureAggregate` snapshots
- Root-cause family classification and cross-fingerprint family aggregation
- Bounded failure timelines with per-minute burst detection
- Top failure reports with configurable limits
- Skip-reason analysis for batch `SkipPolicy` integrations
- Topic, partition, and offset-aware consumer failure tracking
- Spring-managed tracker integration
- `@EnableBugDna` explicit enablement annotation
- Automatic Spring MVC exception fingerprint logging
- Automatic Spring WebFlux exception fingerprint logging
- MDC fingerprint fields
- Actuator recent-fingerprint endpoint
- Micrometer total and unique failure metrics
- Structured documentation under `docs/`
- Core-versus-starter feature and integration comparison
- `bugdna analyze <log-file>` command-line failure summary
- `bugdna compare <old-log-file> <new-log-file>` signature comparison

### Changed

- Automatic Spring logs use a compact `[BUGDNA-*]` prefix
- Spring Boot auto-configuration uses `AutoConfiguration.imports`

## 1.0.1

- Current published project version

Release-specific historical notes before this documentation baseline are available
from the repository's Git history and release pages.
