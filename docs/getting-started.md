# Getting Started

## Choose a Package

### Java

Use the core artifact for plain Java applications (Java 8+):

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna</artifactId>
    <version>1.2.0</version>
</dependency>
```

Gradle:

```groovy
implementation "io.github.arnabnandy7:bugdna:1.2.0"
```

Use the starter for Spring Boot applications (Java 17+, Spring Boot 4.x):

```xml
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-spring-boot-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

Gradle:

```groovy
implementation "io.github.arnabnandy7:bugdna-spring-boot-starter:1.2.0"
```

The starter already depends on the core artifact.

### Node.js / TypeScript

Requires Node.js 18+ (dual ESM and CommonJS with TypeScript definitions):

```bash
npm install bugdna
```

### Python

Requires Python 3.9+ (zero dependencies, `PEP 561` typed):

```bash
pip install bugdna
```

## Generate a Fingerprint

### Java

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

### Node.js / TypeScript

```typescript
import { generate } from 'bugdna';

try {
  runApplicationCode();
} catch (err) {
  const fingerprint = generate(err as Error);
  console.log(fingerprint.id);
}
```

### Python

```python
from bugdna import generate

try:
    run_application_code()
except Exception as exc:
    fingerprint = generate(exc)
    print(fingerprint.id)
```

Example output:

```text
BUGDNA-7A3F21B9E4C018D2
```

Every fingerprint identifier uses 16 uppercase hexadecimal characters after the `BUGDNA-` prefix.

## Inspect the Failure

### Java

```java
System.out.println(fingerprint.getRootCause());
System.out.println(fingerprint.getSignature());
System.out.println(fingerprint.getQualifiedSignature());
System.out.println(fingerprint.getCategory());
System.out.println(fingerprint.getFamily());
System.out.println(fingerprint.getStabilityScore());
```

### Node.js / TypeScript

```typescript
console.log(fingerprint.rootCause);
console.log(fingerprint.signature);
console.log(fingerprint.qualifiedSignature);
console.log(fingerprint.category);
console.log(fingerprint.family);
console.log(fingerprint.stabilityScore);
```

### Python

```python
print(fingerprint.root_cause)
print(fingerprint.signature)
print(fingerprint.qualified_signature)
print(fingerprint.category)
print(fingerprint.family)
print(fingerprint.stability_score)
```

Example output:

```text
java.lang.NullPointerException
UserService#getUser
com.example.UserService#getUser
UNKNOWN
UNKNOWN
90
```

For a log-friendly multi-line summary across any language, call `fingerprint.explain()`:

```text
BUGDNA-7A3F21B9E4C018D2

Root Cause:
NullPointerException

Origin:
UserService#getUser

Confidence:
90%

Failure Chain:
UserController -> UserService
```

## Assert Fingerprints in Automated Tests

### Java

```java
import static io.github.bugdna.BugDnaAssertions.assertThat;

assertThat(fingerprint)
        .hasCategory(FailureCategory.DATABASE)
        .hasRootCause(SQLTimeoutException.class);
```

### Node.js / TypeScript

```typescript
import { BugDnaAssertions, FailureCategory } from 'bugdna';

BugDnaAssertions.assertThat(fingerprint)
  .hasCategory(FailureCategory.DATABASE)
  .hasRootCause('SQLTimeoutException');
```

### Python

```python
from bugdna import BugDnaAssertions, FailureCategory

BugDnaAssertions.assert_that(fingerprint) \
    .has_category(FailureCategory.DATABASE) \
    .has_root_cause( TimeoutError )
```

## Render Causal Dependencies

```java
// Java
System.out.println(BugDna.dependencyGraph(exception).report());
```

```typescript
// Node.js / TypeScript
import { dependencyGraph } from 'bugdna';
console.log(dependencyGraph(err).report());
```

```python
# Python
from bugdna import dependency_graph
print(dependency_graph(exc).report())
```

```text
BUGDNA-001
 └─ BUGDNA-014
      └─ BUGDNA-022
```

## Look Up Runbooks and Ownership

Create a `bugdna.yml` file in your working directory:

```yaml
BUGDNA-001:
  title: Database Pool Exhaustion
  owner: Platform Team
  runbook: runbooks/db-pool.md
```

Look up context by ID in Java (`BugDna.lookup("BUGDNA-001")`), Node.js (`lookup('BUGDNA-001')`), or Python (`lookup("BUGDNA-001")`).

## Track Recurring Failures

### Java

```java
FailureTracker tracker = new FailureTracker();
tracker.capture(exception);

System.out.println(tracker.getTotalOccurrences());
System.out.println(tracker.getUniqueFailures());
System.out.println(tracker.topFailureReport());
```

### Node.js / TypeScript

```typescript
import { FailureTracker } from 'bugdna';

const tracker = new FailureTracker();
tracker.capture(err as Error);

console.log(tracker.getTotalOccurrences());
console.log(tracker.getUniqueFailures());
console.log(tracker.topFailureReport());
```

### Python

```python
from bugdna import FailureTracker

tracker = FailureTracker()
tracker.capture(exc)

print(tracker.total_occurrences)
print(tracker.unique_failures)
print(tracker.top_failure_report())
```

The tracker is in-memory only (and thread-safe in Java and Python). After repeated captures, `tracker.report()` groups occurrences by fingerprint:

```text
2 unique failure signatures

BUGDNA-001
Count: 12

BUGDNA-002
Count: 3
```

## Enable Spring Capture (Java)

The Spring Boot starter participates in Spring Boot auto-configuration. You may also make the integration explicit:

```java
import io.github.bugdna.spring.EnableBugDna;

@EnableBugDna
@SpringBootApplication
class Application {
}
```

Unhandled Spring MVC and WebFlux exceptions then pass through BugDNA without replacing Spring's normal exception handling.

## Next Steps

- Learn fingerprint behavior in [Core library](core-library.md).
- Configure aggregation in [Failure tracking](failure-tracking.md).
- Configure Spring in [Spring Boot starter](spring-boot-starter.md).
- Add metrics, OpenTelemetry, and MDC in [Observability](observability.md).
