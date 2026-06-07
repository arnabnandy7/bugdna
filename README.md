[![Publish to Maven Central](https://github.com/arnabnandy7/bugdna/actions/workflows/publish.yml/badge.svg)](https://github.com/arnabnandy7/bugdna/actions/workflows/publish.yml)[![CodeQL](https://github.com/arnabnandy7/bugdna/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/arnabnandy7/bugdna/actions/workflows/github-code-scanning/codeql)

# bugdna

A Java library that converts exceptions into unique fingerprints for grouping, tracking, and troubleshooting recurring failures.

## Installation

bugdna is available from
[Maven Central](https://central.sonatype.com/artifact/io.github.arnabnandy7/bugdna/overview).
Published files and versions are available in the
[Maven Central repository](https://repo1.maven.org/maven2/io/github/arnabnandy7/bugdna/).
Add the dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.arnabnandy7</groupId>
        <artifactId>bugdna</artifactId>
        <version>0.2.0</version>
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

    System.out.println(fingerprint.getCauseChain());
    // Exception types from the outer failure to the root cause

    System.out.println(fingerprint.getExplanation());
    // Human-readable grouping and priority explanation
}
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

<!-- GitAds-Verify: VNXDGD9D2JN62HPBA3BGQPTVFB1DIQMK -->
