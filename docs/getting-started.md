# Getting Started

## Choose an Artifact

Use the core artifact for plain Java applications:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna</artifactId>
    <version>0.2.4</version>
</dependency>
```

Gradle:

```groovy
implementation "io.github.arnabnandy7:bugdna:0.2.4"
```

Use the starter for Spring Boot applications:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-spring-boot-starter</artifactId>
    <version>0.2.4</version>
</dependency>
```

Gradle:

```groovy
implementation "io.github.arnabnandy7:bugdna-spring-boot-starter:0.2.4"
```

The starter already depends on the core artifact.

## Generate a Fingerprint

```java
import io.github.bugdna.BugDna;
import io.github.bugdna.Fingerprint;

try {
    runApplicationCode();
} catch (Exception exception) {
    Fingerprint fingerprint = BugDna.generate(exception);
    System.out.println(fingerprint.getId());
}
```

Example:

```text
BUGDNA-7A3F21
```

The actual identifier uses 16 uppercase hexadecimal characters after the prefix.

## Inspect the Failure

```java
System.out.println(fingerprint.getRootCause());
System.out.println(fingerprint.getSignature());
System.out.println(fingerprint.getQualifiedSignature());
System.out.println(fingerprint.getCategory());
System.out.println(fingerprint.getStabilityScore());
```

For a log-friendly block:

```java
System.out.println(fingerprint.explain());
```

## Track Recurring Failures

```java
FailureTracker tracker = new FailureTracker();
tracker.capture(exception);

System.out.println(tracker.getTotalOccurrences());
System.out.println(tracker.getUniqueFailures());
System.out.println(tracker.topFailureReport());
```

The tracker is thread-safe and in-memory only.

## Enable Spring Capture

The starter participates in Spring Boot auto-configuration. You may also make the
integration explicit:

```java
import io.github.bugdna.spring.EnableBugDna;

@EnableBugDna
@SpringBootApplication
class Application {
}
```

Unhandled Spring MVC exceptions then pass through BugDNA without replacing Spring's
normal exception handling.

## Next Steps

- Learn fingerprint behavior in [Core library](core-library.md).
- Configure aggregation in [Failure tracking](failure-tracking.md).
- Configure Spring in [Spring Boot starter](spring-boot-starter.md).
- Add metrics and MDC in [Observability](observability.md).
