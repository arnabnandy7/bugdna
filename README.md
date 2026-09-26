[![Publish to Maven Central](https://github.com/arnabnandy7/bugdna/actions/workflows/publish.yml/badge.svg)](https://github.com/arnabnandy7/bugdna/actions/workflows/publish.yml)
[![CodeQL](https://github.com/arnabnandy7/bugdna/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/arnabnandy7/bugdna/actions/workflows/github-code-scanning/codeql)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)

# bugdna

BugDNA converts exceptions across **Java**, **Node.js / TypeScript**, and **Python** into deterministic fingerprints for grouping, tracking, logging, and comparing recurring failures.

```text
BUGDNA-7A3F21B9E4C018D2
```

## Features

- Deterministic exception fingerprints (Java, TypeScript/Node.js, and Python)
- Shared language-neutral specification and cross-language compatibility test suite
- PII-safe normalization for emails and numeric identifiers
- Fingerprint knowledge base lookup for owners and runbooks
- Fluent fingerprint assertions for automated tests
- Root-cause, category, stability, and priority analysis
- Root-cause family clustering across different fingerprints
- Failure dependency graphs from causal chains
- Timestamped failure timelines and burst detection
- Deployment regression detection for new, resolved, and recurring fingerprints
- Fingerprint drift detection for recurring IDs whose signature shape changes
- In-memory concurrent failure aggregation
- Top failure reports for batch jobs
- Most-common skip reason analysis
- Topic-aware consumer failure tracking
- Log-file analysis CLI
- Log-to-log signature comparison
- Fingerprint similarity and regression diffs
- Build-time exception-handling validation for Maven and Gradle
- Spring MVC and WebFlux unhandled-exception capture
- SLF4J MDC integration
- OpenTelemetry span enrichment
- Actuator and Micrometer metrics

## Requirements

| Module | Runtime | Framework |
| --- | --- | --- |
| Core library (`bugdna-core`) | Java 8+ | None |
| Node.js / TypeScript (`bugdna-js`) | Node.js 18+ | None |
| Python (`bugdna-python`) | Python 3.9+ | None |
| Build scanner | Java 8+ | Maven or Gradle |
| Spring Boot starter | Java 17+ | Spring Boot 4.x |
| CLI | Java 8+ | None |

## Installation

### Java (Maven)

Core library:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna</artifactId>
    <version>1.2.0</version>
</dependency>
```

Spring Boot starter:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-spring-boot-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

Maven build-time scanner plugin:

```xml
<plugin>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-maven-plugin</artifactId>
    <version>1.2.0</version>
</plugin>
```

### Java (Gradle)

Core library:

```groovy
implementation "io.github.arnabnandy7:bugdna:1.2.0"
```

Spring Boot starter:

```groovy
implementation "io.github.arnabnandy7:bugdna-spring-boot-starter:1.2.0"
```

Gradle build-time scanner plugin:

```groovy
plugins {
    id 'io.github.arnabnandy7.bugdna' version '1.2.0'
}
```

### Node.js / TypeScript (npm)

```bash
npm install bugdna
```

### Python (PyPI)

```bash
pip install bugdna
```

## Documentation

- [Documentation index](docs/README.md)
- [Getting started](docs/getting-started.md)
- [Core vs Spring Boot starter](docs/core-vs-starter.md)
- [Core library](docs/core-library.md)
- [Failure tracking](docs/failure-tracking.md)
- [Spring Boot starter](docs/spring-boot-starter.md)
- [Build-time validation](docs/build-time-validation.md)
- [Command-line interface](docs/cli.md)
- [Configuration reference](docs/configuration.md)
- [Observability](docs/observability.md)
- [API reference](docs/api-reference.md)
- [Architecture and data handling](docs/architecture.md)
- [Language specification](specification/SPECIFICATION.md)
- [Node.js / TypeScript package](bugdna-js/README.md)
- [Python package](bugdna-python/README.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Migration guide](docs/migration-guide.md)
- [FAQ](docs/faq.md)

## Project

- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTORS.md)
- [License](LICENSE)
- [Core artifact](https://central.sonatype.com/artifact/io.github.arnabnandy7/bugdna/overview)
- [Starter artifact](https://central.sonatype.com/artifact/io.github.arnabnandy7/bugdna-spring-boot-starter/overview)

## GitAds Sponsored

[![Sponsored by GitAds](https://gitads.dev/v1/ad-serve?source=arnabnandy7/bugdna@github)](https://gitads.dev/v1/ad-track?source=arnabnandy7/bugdna@github)

<!-- GitAds-Verify: VNXDGD9D2JN62HPBA3BGQPTVFB1DIQMK -->
