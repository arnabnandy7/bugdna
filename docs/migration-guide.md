# Migration Guide

## Current Compatibility

| Component | Current baseline |
| --- | --- |
| BugDNA core (`io.github.arnabnandy7:bugdna`) | Java 8+ |
| BugDNA Node.js / TypeScript (`bugdna` on npm) | Node.js 18+ |
| BugDNA Python (`bugdna` on PyPI) | Python 3.9+ |
| BugDNA build scanner & Maven/Gradle plugins | Java 8+ |
| BugDNA CLI (`bugdna-cli`) | Java 8+ |
| BugDNA starter (`bugdna-spring-boot-starter`) | Java 17+, Spring Boot 4.x |
| Auto-configuration discovery | `AutoConfiguration.imports` |

## Spring Boot 3 and Newer

Auto-configuration candidates are located in:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

The legacy `EnableAutoConfiguration` key in `META-INF/spring.factories` is not used.

## Adopting `@EnableBugDna`

The starter participates in automatic discovery, so existing applications do not
need the annotation. Add `@EnableBugDna` when explicit configuration imports are
preferred.

## Adopting Failure Tracking

Core (Java, Node.js / TypeScript, Python):

```java
FailureTracker tracker = new FailureTracker();
tracker.capture(exception);
```

Starter (Java Spring Boot):

```java
FailureTracker tracker;
```

Inject the managed bean. Automatic MVC, WebFlux, and service captures feed it.

## Adopting Compact Logs

Automatic logs use:

```text
[BUGDNA-*] Unhandled exception fingerprinted by bugdna
```

Use `bugdna.include-stack-trace=true` when the previous behavior requires a stack
trace.

## Compatibility Checklist

Before upgrading:

1. Run `mvn clean test` (Java) and `./compatibility-tests/run-all.ps1` or `./compatibility-tests/run-all.sh` (cross-language suite).
2. Confirm the language runtime baseline (Java 8+, Java 17+ for Spring Boot starter, Node.js 18+, or Python 3.9+).
3. Review Spring property names and defaults.
4. Confirm management endpoints are explicitly exposed.
5. Treat all in-memory counts as reset during deployment.

See [CHANGELOG.md](https://github.com/arnabnandy7/bugdna/blob/main/CHANGELOG.md) for release-specific changes.
