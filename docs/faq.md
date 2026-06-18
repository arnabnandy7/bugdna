# FAQ

## Does BugDNA Store Exceptions in a Database?

No. `FailureTracker` and `BugDnaFingerprintRepository` keep data in application
memory only. BugDNA does not configure a database or write exception messages to
one.

To retain history, export the Micrometer gauges to a monitoring backend:

```properties
management.endpoints.web.exposure.include=health,prometheus
```

Then scrape:

```text
bugdna_failures_total
bugdna_unique_failures
```

The monitoring backend stores the time series; BugDNA itself still resets on
restart.

## Do Counts Survive Restart?

No. All tracker counts, recent Actuator records, and BugDNA metrics restart from
zero with the application.

For durable history, use one of these patterns:

```java
String report = tracker.report();
auditLog.info("Daily BugDNA report:\n{}", report);
```

or export the Micrometer metrics to Prometheus, Datadog, CloudWatch, or another
monitoring system.

## How Do I Group 500 Failures into Unique Signatures?

Record each exception in the same tracker:

```java
FailureTracker tracker = new FailureTracker();

for (Throwable failure : failures) {
    tracker.capture(failure);
}

System.out.println(tracker.report());
```

Example result:

```text
3 unique failure signatures

BUGDNA-001
Count: 421

BUGDNA-002
Count: 53

BUGDNA-003
Count: 26
```

Grouping uses the fingerprint ID, not the exception message.

## Is the Tracker Available Without Spring?

Yes. Create one application-scoped instance and reuse it:

```java
public final class FailureRegistry {
    private static final FailureTracker TRACKER = new FailureTracker();

    public static void record(Throwable failure) {
        TRACKER.capture(failure);
    }

    public static String report() {
        return TRACKER.report();
    }
}
```

A new tracker per exception will not aggregate counts.

## Is the Tracker Available in Spring?

Yes. Inject the starter-managed singleton:

```java
@Component
class FailureSummary {
    private final FailureTracker tracker;

    FailureSummary(FailureTracker tracker) {
        this.tracker = tracker;
    }

    String currentReport() {
        return tracker.report();
    }
}
```

Automatic MVC captures and `BugDnaSpringService.fingerprint(...)` both update this
tracker.

## How Do I Capture Scheduled or Background Failures?

Automatic capture covers unhandled MVC and WebFlux web exceptions only. Catch
background failures and pass them to `BugDnaSpringService`:

```java
@Component
class InvoiceJob {
    private final BugDnaSpringService bugDna;

    InvoiceJob(BugDnaSpringService bugDna) {
        this.bugDna = bugDna;
    }

    @Scheduled(fixedDelayString = "PT5M")
    void run() {
        try {
            generateInvoices();
        } catch (RuntimeException failure) {
            Fingerprint fingerprint = bugDna.fingerprint(failure);
            log.error("Invoice job failed [{}]", fingerprint.getId(), failure);
        }
    }
}
```

The service updates both recent Actuator records and grouped tracker counts.

## Does the Core Library Modify MDC?

No. `BugDna.generate(...)` has no logging dependency and does not touch MDC.

The Spring starter adds MDC values only around its automatic exception log. To show
the ID:

```properties
logging.pattern.console=%-5level [%X{bugdna}] %logger{36} - %msg%n
```

## Does BugDNA Replace Spring Exception Handling?

No. The MVC resolver records the exception and returns control to Spring. Existing
`@ControllerAdvice`, `@ExceptionHandler`, and default error responses continue to
work.

For example, this handler still owns the HTTP response:

```java
@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<String> notFound(OrderNotFoundException failure) {
        return ResponseEntity.status(404).body(failure.getMessage());
    }
}
```

## Are Exception Messages Used in Fingerprints?

No. Different messages group together when exception type and normalized frames are
the same:

```java
Fingerprint first = BugDna.generate(new IllegalArgumentException("user 17"));
Fingerprint second = BugDna.generate(new IllegalArgumentException("user 42"));

boolean sameGroup = first.getId().equals(second.getId());
```

In real code, `sameGroup` is `true` only when the two exceptions also originate from
the same normalized call path.

## Can Line Number Changes Alter an ID?

No. Line numbers and file names are excluded. Moving the same method from line 20
to line 40 does not change its ID.

Changing the exception type, class, method, or normalized call path can create a new
ID. Use this to explain a change:

```java
FingerprintDiff diff = BugDiff.compare(previousFailure, currentFailure);
System.out.println(diff.explain());
```

## Is `@EnableBugDna` Required?

No. Adding the starter dependency is enough for Spring Boot auto-configuration:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-spring-boot-starter</artifactId>
    <version>1.1.2</version>
</dependency>
```

Use `@EnableBugDna` only when explicit imports are preferred.

## Does Automatic Capture Support WebFlux?

Yes. The starter installs a reactive `WebExceptionHandler` when the application is
a WebFlux web application. Unhandled reactive errors are fingerprinted and logged,
then re-emitted so Spring continues normal error handling.

Scheduled jobs, message listeners, and errors handled inside the reactive chain
still require explicit capture:

```java
return operation()
        .doOnError(bugDna::fingerprint)
        .onErrorResume(this::recover);
```

Use explicit capture when application code consumes the error before it reaches the
global WebFlux exception chain.

## How Do I Get a Top-10 Report?

```java
System.out.println(tracker.topFailureReport());
```

To request a smaller report:

```java
System.out.println(tracker.topFailureReport(3));
```

## How Do I Reset Counts?

```java
tracker.clear();
```

This clears the core tracker only. It does not clear exported monitoring history or
the starter's separate recent-record repository.
