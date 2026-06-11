# BugDNA Documentation

Use this index to choose the shortest path to the information you need.

## Start Here

- [Getting started](getting-started.md): install BugDNA and generate a fingerprint
- [Core library](core-library.md): fingerprinting, priority, similarity, and diffs
- [Failure tracking](failure-tracking.md): in-memory aggregation and top reports
- [Spring Boot starter](spring-boot-starter.md): automatic MVC capture and injection

## Operations

- [Configuration](configuration.md): complete Spring property reference
- [Observability](observability.md): logs, MDC, Actuator, Micrometer, and Prometheus
- [Architecture](architecture.md): determinism, concurrency, lifecycle, and data handling

## Reference

- [API reference](api-reference.md): public types and methods
- [Troubleshooting](troubleshooting.md): common setup and runtime problems
- [Migration guide](migration-guide.md): compatibility and upgrade notes
- [FAQ](faq.md): concise answers to common questions

## Compatibility

| Artifact | Runtime |
| --- | --- |
| `io.github.arnabnandy7:bugdna` | Java 8+ |
| `io.github.arnabnandy7:bugdna-spring-boot-starter` | Java 17+, Spring Boot 4.x |

The core artifact has no Spring or logging dependency. Integrations live in the
starter.
