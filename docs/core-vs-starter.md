# Core vs Spring Boot Starter

BugDNA ships language-neutral core libraries for **Java** (`io.github.arnabnandy7:bugdna`), **Node.js / TypeScript** (`bugdna` on npm), and **Python** (`bugdna` on PyPI), plus a Java **Spring Boot starter** (`io.github.arnabnandy7:bugdna-spring-boot-starter`) that connects `bugdna-core` to Spring MVC, WebFlux, SLF4J, MDC, OpenTelemetry, Actuator, and Micrometer.

## Feature Comparison

| Feature | Core libraries (Java, Node.js / TS, Python) | Spring Boot starter (Java 17+) |
| --- | --- | --- |
| Generate fingerprints | `BugDna.generate(...)` / `generate(...)` | `BugDna.generate(...)` or inject `BugDnaSpringService` |
| Root cause, category, family, priority, and stability | Available | Available through the core API and Spring service |
| PII-safe normalization (`{NUMBER}`, `{EMAIL}`) | `BugDna.normalize(...)` / `normalize(...)` | Same core API |
| Fingerprint knowledge base (`bugdna.yml`) | `BugDna.lookup(...)` / `lookup(...)` | Same core API |
| Fluent test assertions | `BugDnaAssertions.assertThat(...)` / `assert_that(...)` | Same core API |
| Causal dependency graphs | `BugDna.dependencyGraph(...)` / `dependencyGraph(...)` / `dependency_graph(...)` | Same core API |
| Similarity and diffs | `BugSimilarity` / `compareFingerprints` / `compare_fingerprints` and `BugDiff` / `diffErrors` / `diff_exceptions` | Same core APIs; `BugDnaSpringService.diff(...)` is also available |
| Deployment regression and signature drift detection | `RegressionDetector` and `FingerprintDriftDetector` / `detectDrift` / `detect_drift` | Same core APIs |
| Failure grouping, timelines, and burst detection | Create `FailureTracker` | `FailureTracker` is auto-configured as a singleton bean |
| Top failure and root-cause family reports | Available on `FailureTracker` | Available from the injected `FailureTracker` |
| Skip reason analysis | Create `SkipReasonAnalyzer` | Core type is available; define a bean to inject it |
| Consumer failure tracking | Create `ConsumerFailureTracker` | Core type is available; define a bean to inject it |
| Automatic MVC exception capture | Not available | Automatic for unhandled servlet MVC exceptions |
| WebFlux automatic capture | Not available | Automatic for unhandled reactive web exceptions |
| Background and scheduled failures | Capture manually | Call `BugDnaSpringService.fingerprint(...)` manually |
| Automatic SLF4J logging | Not available | Available for automatic MVC and WebFlux capture |
| MDC fields | Not available | Available during automatic MVC and WebFlux log calls |
| OpenTelemetry span enrichment | Not available | Enriches active spans when `opentelemetry-api` is present |
| Recent fingerprint repository | Not available | Auto-configured in memory |
| Actuator endpoint | Not available | Registered when Actuator is present; HTTP access requires exposure |
| Micrometer metrics | Not available | Auto-configured when a `MeterRegistry` is present |
| Persistent storage | Not included | Not included |

## Bean Registration (Spring Boot Starter)

The starter auto-configures these primary application-facing beans:

| Bean | Purpose |
| --- | --- |
| `FailureTracker` | Shared grouped occurrence counts, families, timelines, and bursts |
| `BugDnaSpringService` | Fingerprints failures, enriches active OpenTelemetry spans, and updates starter state |
| `BugDnaFingerprintRepository` | Bounded recent records and process counters |
| `BugDnaEndpoint` | Actuator endpoint when Actuator is available |

It also conditionally registers integration beans:

| Bean | Condition |
| --- | --- |
| MVC `HandlerExceptionResolver` | Servlet MVC is on the classpath and logging is enabled |
| WebFlux `WebExceptionHandler` | Reactive WebFlux is on the classpath and logging is enabled |
| `BugDnaSpanEnricher` | OpenTelemetry API is on the classpath and `bugdna.otel-enabled=true` |
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

## Choosing a Package

Use a core package (`io.github.arnabnandy7:bugdna` for Java 8+, `bugdna` on npm for Node.js 18+ / TypeScript, or `bugdna` on PyPI for Python 3.9+) when:

- The application runs on Node.js, TypeScript, Python, or non-Spring Java
- Java 8 compatibility is required
- The application owns capture, logging, and metrics
- Only deterministic fingerprinting and in-memory analysis are needed

Use the Spring Boot starter (`io.github.arnabnandy7:bugdna-spring-boot-starter`) when:

- The application uses Java 17+ and Spring Boot 4.x
- Automatic servlet MVC or reactive WebFlux capture is useful
- BugDNA services and the shared tracker should be injectable
- Actuator, Micrometer, OpenTelemetry span enrichment, logging, or MDC integration is needed

Do not add both Java dependencies explicitly. The starter already includes the core artifact.

The CLI is a separate artifact. It analyzes `BUGDNA-*` IDs in existing log files produced by any language runtime and does not change core or starter runtime behavior. See the [command-line guide](cli.md).
