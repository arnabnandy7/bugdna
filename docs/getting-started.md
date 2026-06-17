# Getting Started

## Choose an Artifact

Use the core artifact for plain Java applications:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna</artifactId>
    <version>1.1.1</version>
</dependency>
```

Gradle:

```groovy
implementation "io.github.arnabnandy7:bugdna:1.1.1"
```

Use the starter for Spring Boot applications:

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-spring-boot-starter</artifactId>
    <version>1.1.1</version>
</dependency>
```

Gradle:

```groovy
implementation "io.github.arnabnandy7:bugdna-spring-boot-starter:1.1.1"
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

Example output:

```text
java.lang.NullPointerException
UserService#getUser
com.example.UserService#getUser
UNKNOWN
90
```

For a log-friendly block:

```java
System.out.println(fingerprint.explain());
```

```text
BUGDNA-7A3F21

Root Cause:
NullPointerException

Origin:
UserService#getUser

Confidence:
90%

Failure Chain:
UserController -> UserService
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

After repeated captures, `tracker.report()` groups occurrences by fingerprint:

```text
2 unique failure signatures

BUGDNA-001
Count: 12

BUGDNA-002
Count: 3
```

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

Unhandled Spring MVC and WebFlux exceptions then pass through BugDNA without
replacing Spring's normal exception handling.

## Next Steps

- Learn fingerprint behavior in [Core library](core-library.md).
- Configure aggregation in [Failure tracking](failure-tracking.md).
- Configure Spring in [Spring Boot starter](spring-boot-starter.md).
- Add metrics and MDC in [Observability](observability.md).
