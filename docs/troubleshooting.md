# Troubleshooting

## No Fingerprint Is Logged

Check:

1. The Spring starter dependency is present.
2. The application is a servlet Spring MVC application.
3. `bugdna.enabled` is not `false`.
4. `bugdna.log-enabled` is not `false`.
5. Error logging is enabled for `io.github.bugdna.spring.BugDnaExceptionLogger`.

Automatic capture does not currently cover WebFlux or non-web background exceptions.
Use `BugDnaSpringService` or `FailureTracker` manually for those paths.

## `FailureTracker` Bean Is Missing

Confirm core BugDNA auto-configuration is enabled. The bean is not created when:

```properties
bugdna.enabled=false
```

Applications may also provide their own `FailureTracker` bean.

## MDC Value Is Empty

MDC keys are present only during BugDNA's automatic log call and are removed
afterward. Include `%X{bugdna}` in the logging pattern:

```properties
logging.pattern.console=%-5level [%X{bugdna}] %logger{36} - %msg%n
```

Direct calls to `BugDna.generate(...)` do not modify MDC.

## Actuator Endpoint Returns 404

Creating an endpoint and exposing it are separate steps:

```properties
management.endpoints.web.exposure.include=health,bugdna
```

Also verify that Spring Boot Actuator is on the classpath.

## Prometheus Metrics Are Missing

Check that:

- A Micrometer `MeterRegistry` bean exists
- The Prometheus registry dependency is installed
- The Prometheus endpoint is exposed
- At least one failure has been recorded

Prometheus names use underscores:

```text
bugdna_failures_total
bugdna_unique_failures
```

## Counts Reset

This is expected after restart. Trackers, recent snapshots, and metrics are all
in-memory. Export metrics to monitoring storage when historical retention is needed.

## Similar Failures Have Different IDs

Class names, method names, and normalized call paths affect identity. Compare the
fingerprints with `BugDiff.compare(...)` or `BugSimilarity.compare(...)`.

## Maven Cannot Resolve Spring Boot Dependencies

Verify network access to Maven Central and confirm the configured Spring Boot version
exists. The starter build requires the Spring Boot dependency BOM.
