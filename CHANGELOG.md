# Changelog

Notable project changes are documented here.

## Unreleased

### Added

- Core in-memory `FailureTracker`
- Immutable `FailureAggregate` snapshots
- Top failure reports with configurable limits
- Spring-managed tracker integration
- `@EnableBugDna` explicit enablement annotation
- Automatic Spring MVC exception fingerprint logging
- MDC fingerprint fields
- Actuator recent-fingerprint endpoint
- Micrometer total and unique failure metrics
- Structured documentation under `docs/`

### Changed

- Automatic Spring logs use a compact `[BUGDNA-*]` prefix
- Spring Boot auto-configuration uses `AutoConfiguration.imports`

## 0.2.3

- Current published project version

Release-specific historical notes before this documentation baseline are available
from the repository's Git history and release pages.
