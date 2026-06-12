# Spring Boot Starter

The starter adds automatic MVC and WebFlux exception capture plus Spring-managed
BugDNA services.

See [Core vs Spring Boot starter](core-vs-starter.md) for a feature-by-feature
comparison, including which core types require application-defined beans.

## Requirements

- Java 17 or newer
- Spring Boot 4.x
- Spring MVC or WebFlux for automatic web exception capture

## Setup

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-spring-boot-starter</artifactId>
    <version>1.0.1</version>
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

## Automatic WebFlux Capture

In a reactive web application, BugDNA registers a highest-precedence
`WebExceptionHandler`. It fingerprints and logs the exception, then re-emits the
same failure so Spring WebFlux continues its normal error handling.

No controller or reactive-chain changes are required:

```java
@RestController
class PaymentController {

    @GetMapping("/payments/{id}")
    Mono<Payment> payment(@PathVariable String id) {
        return paymentService.find(id);
    }
}
```

An unhandled error emitted by `paymentService.find(id)` is captured automatically.
The same `bugdna.log-enabled`, `bugdna.mdc-enabled`, and
`bugdna.include-stack-trace` properties apply to MVC and WebFlux.

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

### Background Job Example

Automatic web capture does not cover scheduled jobs, listeners, or manually created
threads. Record those failures explicitly:

```java
@Component
class PaymentReconciliationJob {
    private final BugDnaSpringService bugDna;

    PaymentReconciliationJob(BugDnaSpringService bugDna) {
        this.bugDna = bugDna;
    }

    @Scheduled(fixedDelayString = "PT10M")
    void reconcile() {
        try {
            reconcilePayments();
        } catch (RuntimeException failure) {
            Fingerprint fingerprint = bugDna.fingerprint(failure);
            log.error("Reconciliation failed [{}]", fingerprint.getId(), failure);
        }
    }
}
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

Example result:

```text
Top 10 Failure Signatures
BUGDNA-001
Count: 421
BUGDNA-002
Count: 53
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

Keep automatic capture but suppress stack traces:

```properties
bugdna.log-enabled=true
bugdna.include-stack-trace=false
```

See [Configuration](configuration.md) for every property.
