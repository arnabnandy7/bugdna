[![Publish to Maven Central](https://github.com/arnabnandy7/bugdna/actions/workflows/publish.yml/badge.svg)](https://github.com/arnabnandy7/bugdna/actions/workflows/publish.yml)[![CodeQL](https://github.com/arnabnandy7/bugdna/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/arnabnandy7/bugdna/actions/workflows/github-code-scanning/codeql)[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=bugs)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=coverage)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=arnabnandy7_bugdna&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=arnabnandy7_bugdna)

# bugdna

A Java library that converts exceptions into unique fingerprints for grouping, tracking, and troubleshooting recurring failures.

## Installation

bugdna publishes separate Maven artifacts for the core library and Spring Boot starter:

| Artifact | Maven Central | Repository files |
| --- | --- | --- |
| `io.github.arnabnandy7:bugdna` | [Artifact page](https://central.sonatype.com/artifact/io.github.arnabnandy7/bugdna/overview) | [Repository directory](https://repo1.maven.org/maven2/io/github/arnabnandy7/bugdna/) |
| `io.github.arnabnandy7:bugdna-spring-boot-starter` | [Artifact page](https://central.sonatype.com/artifact/io.github.arnabnandy7/bugdna-spring-boot-starter/overview) | [Repository directory](https://repo1.maven.org/maven2/io/github/arnabnandy7/bugdna-spring-boot-starter/) |

Add the core dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.arnabnandy7</groupId>
        <artifactId>bugdna</artifactId>
        <version>0.2.3</version>
    </dependency>
</dependencies>
```

For Spring Boot applications, use the starter:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.arnabnandy7</groupId>
        <artifactId>bugdna-spring-boot-starter</artifactId>
        <version>0.2.3</version>
    </dependency>
</dependencies>
```

## Usage

```java
import io.github.bugdna.BugDna;
import io.github.bugdna.Fingerprint;

try {
    // Application code
} catch (Exception exception) {
    Fingerprint fingerprint = BugDna.generate(exception);

    System.out.println(fingerprint.getId());
    // BUGDNA-7A3F21

    System.out.println(fingerprint.getRootCause());
    // java.lang.NullPointerException

    System.out.println(fingerprint.getSignature());
    // UserService#getUser

    System.out.println(fingerprint.getQualifiedSignature());
    // com.example.UserService#getUser

    System.out.println(fingerprint.getFrames());
    // Normalized stack frames used for grouping

    System.out.println(fingerprint.getFailureChain());
    // Controller -> Service -> Repository

    System.out.println(fingerprint.getCategory());
    // DATABASE

    System.out.println(fingerprint.getCauseChain());
    // Exception types from the outer failure to the root cause

    System.out.println(fingerprint.getExplanation());
    // Human-readable grouping and priority explanation

    System.out.println(fingerprint.getStabilityScore());
    // 98
}
```

Print a compact explanation in logs:

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
98%

Failure Chain:
Controller -> Service -> Repository
```

Supply operational impact when you want bugdna to prioritize a failure:

```java
import io.github.bugdna.FailureContext;

FailureContext context = FailureContext.of(
        125,   // occurrences
        18,    // affected users
        false  // fatal
);

Fingerprint fingerprint = BugDna.generate(exception, context);

System.out.println(fingerprint.getPriority());
// HIGH
```

Compare two fingerprints to find related failure families:

```java
import io.github.bugdna.BugSimilarity;
import io.github.bugdna.Similarity;

Similarity similarity = BugSimilarity.compare(firstFingerprint, secondFingerprint);

System.out.println(similarity.getPercentage());
// 92

System.out.println(similarity.isLikelyRelated());
// true

System.out.println(similarity.getExplanation());
// Similarity 92% between BUGDNA-... and BUGDNA-...
```

Compare old and new failures to explain what changed:

```java
import io.github.bugdna.BugDiff;
import io.github.bugdna.FingerprintDiff;

FingerprintDiff diff = BugDiff.compare(oldException, newException);

System.out.println(diff.explain());
```

```text
Repository Layer Changed

Old:
UserRepository

New:
CustomerRepository
```

## Spring Boot Starter

The Spring Boot starter auto-configures bugdna without adding Spring dependencies to the
core library.

The starter targets Spring Boot 4.x and requires Java 17 or newer.

```properties
bugdna.enabled=true
bugdna.log-enabled=true
bugdna.mdc-enabled=true
bugdna.include-stack-trace=false
bugdna.recent-limit=50
bugdna.actuator.enabled=true
```

When Spring MVC is present, unhandled web exceptions are fingerprinted and logged
without replacing Spring's normal exception handling. During those logs, the starter
adds MDC fields for `bugdna.id`, `bugdna.confidence`, `bugdna.category`, and
`bugdna.priority`.

You can also inject the Spring service directly:

```java
import io.github.bugdna.Fingerprint;
import io.github.bugdna.spring.BugDnaSpringService;

class FailureReporter {

    private final BugDnaSpringService bugDna;

    FailureReporter(BugDnaSpringService bugDna) {
        this.bugDna = bugDna;
    }

    Fingerprint report(Throwable failure) {
        return bugDna.fingerprint(failure);
    }
}
```

If Spring Boot Actuator is present, recent fingerprints are exposed through the
`bugdna` actuator endpoint.

## GitAds Sponsored
[![Sponsored by GitAds](https://gitads.dev/v1/ad-serve?source=arnabnandy7/bugdna@github)](https://gitads.dev/v1/ad-track?source=arnabnandy7/bugdna@github)



<!-- GitAds-Verify: VNXDGD9D2JN62HPBA3BGQPTVFB1DIQMK -->
