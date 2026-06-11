# Spring Boot Starter

The starter adds automatic MVC exception capture and Spring-managed BugDNA services.

## Requirements

- Java 17 or newer
- Spring Boot 4.x
- Spring MVC for automatic web exception capture

## Setup

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-spring-boot-starter</artifactId>
    <version>0.2.3</version>
</dependency>
```

The starter is discovered through Spring Boot's
`AutoConfiguration.imports` mechanism.

`@EnableBugDna` is available when explicit opt-in is preferred:

```java
@EnableBugDna
@SpringBootApplication
class Application {
}
```

## Automatic MVC Capture

When enabled in a servlet web application, BugDNA registers a
`HandlerExceptionResolver` at highest precedence. It fingerprints and logs the
exception, then returns `null` so Spring continues its normal resolution flow.

Default log shape:

```text
ERROR [BUGDNA-7A3F21] Unhandled exception fingerprinted by bugdna
```

Stack traces are disabled by default and can be enabled with
`bugdna.include-stack-trace=true`.

## Inject the Service

```java
@Service
class FailureReporter {

    private final BugDnaSpringService bugDna;

    FailureReporter(BugDnaSpringService bugDna) {
        this.bugDna = bugDna;
    }

    Fingerprint capture(Throwable failure) {
        return bugDna.fingerprint(failure);
    }
}
```

The service records the fingerprint in the recent repository and shared
`FailureTracker`.

Operational context is also supported:

```java
bugDna.fingerprint(
        exception,
        FailureContext.of(100, 10, false)
);
```

## Inject the Tracker

```java
@Component
class FailureReport {

    private final FailureTracker tracker;

    FailureReport(FailureTracker tracker) {
        this.tracker = tracker;
    }

    String topFailures() {
        return tracker.topFailureReport();
    }
}
```

## Override Beans

Core starter beans use `@ConditionalOnMissingBean`. Applications may provide custom
beans for `FailureTracker`, `BugDnaSpringService`, or
`BugDnaFingerprintRepository`.

## Disable Features

```properties
bugdna.enabled=false
```

Disable only automatic exception logging:

```properties
bugdna.log-enabled=false
```

See [Configuration](configuration.md) for every property.
