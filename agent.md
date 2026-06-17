# Agent Instructions

This repository is a small Java library split into core, CLI, and Spring Boot starter modules. Favor compatibility, predictable behavior, and focused changes over broad redesigns.

## Backward Compatibility

- Treat public classes, method signatures, Maven coordinates, generated fingerprint IDs, CLI commands, and documented behavior as compatibility-sensitive.
- Do not remove or rename public APIs unless the user explicitly asks for a breaking change.
- Keep `bugdna-core` Java 8 compatible. Do not introduce APIs, language features, or dependencies that require a newer Java runtime in the core module.
- Preserve deterministic fingerprinting behavior. Any change that can alter existing fingerprints, diffs, grouping, reports, or timelines needs focused tests and an explicit reason.
- Add overloads, adapters, or optional behavior when extending features, instead of changing existing call sites or defaults.
- Keep Spring Boot starter behavior opt-in where possible and avoid surprising application startup, logging, actuator, or Micrometer behavior changes.
- Update docs and tests when behavior changes, especially for public API, CLI output, or fingerprint semantics.

## Avoid Overengineering

- Prefer small, direct changes that match the current code style.
- Do not add new frameworks, processors, reflection-heavy systems, global registries, or abstractions unless they solve a current, demonstrated problem.
- Avoid speculative extension points. Add interfaces or configuration only when there is an immediate use case in the codebase or request.
- Keep module boundaries simple: core should stay framework-free, CLI should remain thin, and Spring-specific code should stay in the starter.
- Prefer immutable/simple value objects and clear static helpers where that matches existing code.
- Keep tests focused on observable behavior rather than implementation details.

## Change Checklist

- Does this preserve existing public APIs and defaults?
- Could this change alter a previously generated fingerprint or report?
- Does the core module still compile and run on Java 8?
- Is this the smallest maintainable design for the requested behavior?
- Are tests and docs updated for any user-visible behavior change?
