# Migration Guide

## Current Compatibility

| Component | Current baseline |
| --- | --- |
| BugDNA core | Java 8+ |
| BugDNA starter | Java 17+, Spring Boot 4.x |
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

Core:

```java
FailureTracker tracker = new FailureTracker();
tracker.capture(exception);
```

Starter:

```java
FailureTracker tracker;
```

Inject the managed bean. Automatic MVC and service captures feed it.

## Adopting Compact Logs

Automatic logs use:

```text
[BUGDNA-*] Unhandled exception fingerprinted by bugdna
```

Use `bugdna.include-stack-trace=true` when the previous behavior requires a stack
trace.

## Compatibility Checklist

Before upgrading:

1. Run `mvn clean test`.
2. Confirm the Java runtime baseline.
3. Review Spring property names and defaults.
4. Confirm management endpoints are explicitly exposed.
5. Treat all in-memory counts as reset during deployment.

See [CHANGELOG.md](../CHANGELOG.md) for release-specific changes.
