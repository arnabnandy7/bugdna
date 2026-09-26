# BugDNA Documentation

BugDNA provides deterministic exception fingerprinting, similarity comparison, regression and drift detection, and in-memory failure tracking across **Java**, **Node.js / TypeScript**, and **Python**.

Use this index to choose the shortest path to the information you need.

## Start Here

- [Getting started](getting-started.md): install BugDNA and generate fingerprints in Java, Node.js / TypeScript, and Python
- [Core vs starter](core-vs-starter.md): language-neutral core features versus the Java Spring Boot starter
- [Core library](core-library.md): fingerprinting, knowledge base, priority, categories, families, similarity, regression detection, and diffs
- [Failure tracking](failure-tracking.md): in-memory aggregation, timelines, bursts, assertions, dependency graphs, skip reasons, and consumer tracking
- [Spring Boot starter](spring-boot-starter.md): automatic MVC and WebFlux capture and Spring bean injection (Java)
- [Command-line interface](cli.md): analyze and compare `BUGDNA-*` fingerprint counts in existing log files
- [Build-time validation](build-time-validation.md): scan Java source for exception-handling hazards (Maven and Gradle)

## Operations

- [Configuration](configuration.md): Spring properties and cross-language knowledge base configuration
- [Observability](observability.md): logs, MDC, OpenTelemetry, Actuator, Micrometer, and Prometheus
- [Architecture](architecture.md): determinism, concurrency, module boundaries, lifecycle, and data handling

## Reference

- [API reference](api-reference.md): public types and functions across Java, Node.js / TypeScript, and Python
- [Language specification](specification.md): canonical cross-language algorithm and JSON schemas
- [Node.js / TypeScript package](bugdna-js.md): npm package overview (`bugdna-js`)
- [Python package](bugdna-python.md): PyPI package overview (`bugdna-python`)
- [Troubleshooting](troubleshooting.md): common setup and runtime problems
- [Migration guide](migration-guide.md): compatibility and upgrade notes
- [FAQ](faq.md): concise answers to common questions

## Compatibility

| Package / Artifact | Ecosystem | Runtime |
| --- | --- | --- |
| `io.github.arnabnandy7:bugdna` | Maven Central | Java 8+ |
| `bugdna` (`bugdna-js`) | npm | Node.js 18+ / TypeScript |
| `bugdna` (`bugdna-python`) | PyPI | Python 3.9+ |
| `io.github.arnabnandy7:bugdna-spring-boot-starter` | Maven Central | Java 17+, Spring Boot 4.x |
| `io.github.arnabnandy7:bugdna-build-scanner` | Maven Central | Java 8+ |
| `io.github.arnabnandy7:bugdna-maven-plugin` | Maven Central | Java 8+, Maven |
| `io.github.arnabnandy7.bugdna` (`bugdna-gradle-plugin`) | Gradle | Java 8+, Gradle |
| `io.github.arnabnandy7:bugdna-cli` | Maven / JAR | Java 8+ |

The core libraries (`bugdna` on Maven Central, npm, and PyPI) have zero external runtime dependencies. Spring and observability integrations live in `bugdna-spring-boot-starter`.
