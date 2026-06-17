# Claude Instructions

Follow the repository guidance in `agent.md`. The most important constraints are backward compatibility and avoiding unnecessary abstraction.

## Priorities

- Preserve public APIs, Maven coordinates, CLI commands, documented defaults, and deterministic fingerprint behavior.
- Keep `bugdna-core` compatible with Java 8.
- Make focused changes that match the existing style instead of redesigning the library.
- Do not introduce new frameworks, broad extension systems, or speculative abstractions without a concrete need.
- Prefer additive changes, overloads, and optional behavior over breaking changes.
- Add focused tests for changes that affect fingerprints, diffs, reports, trackers, CLI output, or Spring Boot integration behavior.

When in doubt, choose the smallest compatible implementation and call out any compatibility risk explicitly.
