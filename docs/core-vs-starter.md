# Core vs Spring Boot Starter

The Spring Boot starter depends on the core artifact. A starter application can use
every core type, but only some types are registered as Spring beans or connected to
automatic capture.

## Feature Comparison

| Feature | Core library | Spring Boot starter |
| --- | --- | --- |
| Generate fingerprints | Call `BugDna.generate(...)` | Call `BugDna.generate(...)` or inject `BugDnaSpringService` |
| Root cause, category, priority, and stability | Available | Available through the core API and Spring service |
| Similarity and diffs | Call `BugSimilarity` and `BugDiff` | Same core APIs; `BugDnaSpringService.diff(...)` is also available |
| Failure grouping | Create `FailureTracker` | `FailureTracker` is auto-configured as a singleton bean |
| Top failure reports | Available | Available from the injected `FailureTracker` |
| Skip reason analysis | Create `SkipReasonAnalyzer` | Core type is available, but define a bean to inject it |
| Consumer failure tracking | Create `ConsumerFailureTracker` | Core type is available, but define a bean to inject it |
| Automatic MVC exception capture | Not available | Automatic for unhandled servlet MVC exceptions |
| Background and scheduled failures | Capture manually | Call `BugDnaSpringService.fingerprint(...)` manually |
| WebFlux automatic capture | Not available | Automatic for unhandled reactive web exceptions |
| Automatic SLF4J logging | Not available | Available for automatic MVC capture |
| MDC fields | Not available | Available during the automatic MVC log call |
| Recent fingerprint repository | Not available | Auto-configured in memory |
| Actuator endpoint | Not available | Registered when Actuator is present; HTTP access requires exposure |
| Micrometer metrics | Not available | Auto-configured when a `MeterRegistry` is present |
| Persistent storage | Not included | Not included |

## Bean Registration

The starter auto-configures these primary application-facing beans:

| Bean | Purpose |
| --- | --- |
| `FailureTracker` | Shared grouped occurrence counts |
| `BugDnaSpringService` | Fingerprints failures and updates starter state |
| `BugDnaFingerprintRepository` | Bounded recent records and process counters |
| `BugDnaEndpoint` | Actuator endpoint when Actuator is available |

It also conditionally registers integration beans:

| Bean | Condition |
| --- | --- |
| MVC `HandlerExceptionResolver` | Servlet MVC is on the classpath and logging is enabled |
| WebFlux `WebExceptionHandler` | Reactive WebFlux is on the classpath and logging is enabled |
| BugDNA metrics binder | A Micrometer `MeterRegistry` bean exists |

Creating `BugDnaEndpoint` does not expose it over HTTP. Exposure remains a Spring
Boot management setting:

```properties
management.endpoints.web.exposure.include=health,bugdna
```

These core types are available on the starter classpath but are not auto-configured:

| Type | Registration |
| --- | --- |
| `SkipReasonAnalyzer` | Construct directly or define an application bean |
| `ConsumerFailureTracker` | Construct directly or define an application bean |

Register injectable instances when needed:

```java
@Configuration
class BugDnaApplicationConfiguration {

    @Bean
    SkipReasonAnalyzer skipReasonAnalyzer() {
        return new SkipReasonAnalyzer();
    }

    @Bean
    ConsumerFailureTracker consumerFailureTracker() {
        return new ConsumerFailureTracker();
    }
}
```

## Fingerprint Generation

### Core

```java
Fingerprint fingerprint = BugDna.generate(failure);
```

This generates a fingerprint only. It does not update a tracker, repository, log,
MDC, or metric.

### Starter

```java
@Service
class FailureCapture {
    private final BugDnaSpringService bugDna;

    FailureCapture(BugDnaSpringService bugDna) {
        this.bugDna = bugDna;
    }

    Fingerprint capture(Throwable failure) {
        return bugDna.fingerprint(failure);
    }
}
```

The service generates the fingerprint and updates the shared `FailureTracker` and
recent repository. It does not automatically log manual service calls.

## Failure Grouping

### Core

Create and retain one tracker for the desired application scope:

```java
FailureTracker tracker = new FailureTracker();
tracker.capture(failure);
```

Creating a new tracker for every exception prevents aggregation.

### Starter

Inject the shared tracker:

```java
@Component
class FailureReport {
    private final FailureTracker tracker;

    FailureReport(FailureTracker tracker) {
        this.tracker = tracker;
    }

    String report() {
        return tracker.report();
    }
}
```

Automatic MVC captures and `BugDnaSpringService.fingerprint(...)` feed this bean.

## Skip Reason Analysis

The analyzer behaves the same in both artifacts. The difference is ownership.

### Core

```java
SkipReasonAnalyzer analyzer = new SkipReasonAnalyzer();
analyzer.record(skippedFailure);
```

### Starter

Define a bean, then inject it into a Spring Batch `SkipPolicy`:

```java
@Bean
SkipReasonAnalyzer skipReasonAnalyzer() {
    return new SkipReasonAnalyzer();
}
```

BugDNA does not add a Spring Batch dependency or install a `SkipPolicy`
automatically.

## Consumer Failure Tracking

The tracker is messaging-client-neutral in both artifacts.

### Core

```java
ConsumerFailureTracker tracker = new ConsumerFailureTracker();
tracker.capture(topic, partition, offset, failure);
```

### Starter

Define and inject a bean:

```java
@Bean
ConsumerFailureTracker consumerFailureTracker() {
    return new ConsumerFailureTracker();
}
```

```java
@Component
class PaymentConsumer {
    private final ConsumerFailureTracker failures;

    PaymentConsumer(ConsumerFailureTracker failures) {
        this.failures = failures;
    }

    void onFailure(ConsumerRecord<?, ?> record, Throwable failure) {
        failures.capture(
                record.topic(),
                record.partition(),
                record.offset(),
                failure
        );
    }
}
```

The starter does not install Kafka, RabbitMQ, or another consumer interceptor.
Applications pass topic, partition, and offset explicitly.

## Automatic Behavior

Adding the core dependency performs no automatic runtime work.

Adding the starter enables auto-configuration by default:

```properties
bugdna.enabled=true
```

In a servlet MVC or reactive WebFlux application, an unhandled exception is
fingerprinted, recorded, and logged before Spring continues its normal exception
handling. Actuator and Micrometer integrations activate only when their required
classes and beans are present.

## Choosing an Artifact

Use the core artifact when:

- The application is not Spring Boot
- Java 8 compatibility is required
- The application owns capture, logging, and metrics
- Only deterministic fingerprinting and in-memory analysis are needed

Use the starter when:

- The application uses Java 17+ and Spring Boot 4.x
- Automatic servlet MVC or reactive WebFlux capture is useful
- BugDNA services and the shared tracker should be injectable
- Actuator, Micrometer, logging, or MDC integration is needed

Do not add both dependencies explicitly. The starter already includes the core
artifact.

The CLI is a separate artifact. It analyzes BugDNA IDs in existing log files and
does not change core or starter runtime behavior. See the
[command-line guide](cli.md).
