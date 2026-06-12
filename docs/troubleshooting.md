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

```java
try {
    runBackgroundTask();
} catch (RuntimeException failure) {
    Fingerprint fingerprint = bugDna.fingerprint(failure);
    log.error("Background task failed [{}]", fingerprint.getId(), failure);
}
```

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

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Then test the endpoint directly:

```bash
curl -i http://localhost:8080/actuator/bugdna
```

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

Required registry dependency:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Verify the raw endpoint:

```bash
curl http://localhost:8080/actuator/prometheus
```

## Counts Reset

This is expected after restart. Trackers, recent snapshots, and metrics are all
in-memory. Export metrics to monitoring storage when historical retention is needed.

## Similar Failures Have Different IDs

Class names, method names, and normalized call paths affect identity. Compare the
fingerprints with `BugDiff.compare(...)` or `BugSimilarity.compare(...)`.

```java
FingerprintDiff diff = BugDiff.compare(firstFailure, secondFailure);
System.out.println(diff.explain());

Similarity similarity = BugSimilarity.compare(
        BugDna.generate(firstFailure),
        BugDna.generate(secondFailure)
);
System.out.println(similarity.getExplanation());
```

## Maven Cannot Resolve Spring Boot Dependencies

Run Maven with errors enabled:

```bash
mvn -e clean test
```

If the error names `repo.maven.apache.org`, test Maven Central from the same machine:

```bash
curl -I https://repo.maven.apache.org/maven2/
```

Then check the Spring Boot BOM version configured in the root `pom.xml`:

```xml
<spring-boot.version>4.0.6</spring-boot.version>
```

Corporate proxy or repository-mirror settings belong in `~/.m2/settings.xml`. A
`401` or `403` usually indicates repository credentials or mirror policy; a DNS or
connection timeout indicates network or proxy configuration.
