[![Publish to Maven Central](https://github.com/arnabnandy7/bugdna/actions/workflows/publish.yml/badge.svg)](https://github.com/arnabnandy7/bugdna/actions/workflows/publish.yml)
[![CodeQL](https://github.com/arnabnandy7/bugdna/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/arnabnandy7/bugdna/actions/workflows/github-code-scanning/codeql)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)

# bugdna

BugDNA converts Java exceptions into deterministic fingerprints for grouping,
tracking, logging, and comparing recurring failures.

```text
BUGDNA-7A3F21
```

## Features

- Deterministic exception fingerprints
- Root-cause, category, stability, and priority analysis
- Root-cause family clustering across different fingerprints
- In-memory concurrent failure aggregation
- Top failure reports for batch jobs
- Most-common skip reason analysis
- Topic-aware consumer failure tracking
- Log-file analysis CLI
- Log-to-log signature comparison
- Fingerprint similarity and regression diffs
- Spring MVC and WebFlux unhandled-exception capture
- SLF4J MDC integration
- Actuator and Micrometer metrics

## Requirements

| Module | Java | Framework |
| --- | --- | --- |
| Core library | 8+ | None |
| Spring Boot starter | 17+ | Spring Boot 4.x |
| CLI | 8+ | None |

## Installation

Core library:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna</artifactId>
    <version>1.0.1</version>
</dependency>
```

Spring Boot starter:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

CLI:

```bash
mvn -pl bugdna-cli clean package
bin/bugdna analyze app.log
```

Gradle coordinates are available in the [getting-started guide](docs/getting-started.md).

## Quick Start

```java
Fingerprint fingerprint = BugDna.generate(exception);

System.out.println(fingerprint.getId());
System.out.println(fingerprint.explain());
```

Track recurring failures:

```java
FailureTracker tracker = new FailureTracker();
tracker.capture(exception);

System.out.println(tracker.topFailureReport());
```

Spring Boot:

```java
@EnableBugDna
@SpringBootApplication
class Application {
}
```

Unhandled Spring MVC and WebFlux exceptions are fingerprinted, logged, aggregated,
and exposed to optional Actuator and Micrometer integrations.

## Documentation

- [Documentation index](docs/README.md)
- [Getting started](docs/getting-started.md)
- [Core vs Spring Boot starter](docs/core-vs-starter.md)
- [Core library](docs/core-library.md)
- [Failure tracking](docs/failure-tracking.md)
- [Spring Boot starter](docs/spring-boot-starter.md)
- [Command-line interface](docs/cli.md)
- [Configuration reference](docs/configuration.md)
- [Observability](docs/observability.md)
- [API reference](docs/api-reference.md)
- [Architecture and data handling](docs/architecture.md)
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
