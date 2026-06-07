# bugdna

A lightweight Java library that converts noisy stack traces into deterministic
failure signatures.

Most monitoring tools tell you how many exceptions happened. bugdna tells you
how many unique failures happened.

```text
NullPointerException at UserService.java:57
NullPointerException at UserService.java:58
NullPointerException at UserService.java:59
```

These exceptions produce one stable failure fingerprint because line numbers
and exception messages are excluded:

```text
BUGDNA-7A3F21
```

## Usage

```java
import io.github.bugdna.BugDna;
import io.github.bugdna.Fingerprint;

Fingerprint fingerprint = BugDna.generate(exception);

fingerprint.getId();        // BUGDNA-7A3F21
fingerprint.getRootCause(); // java.lang.NullPointerException
fingerprint.getSignature(); // UserService#getUser
```

The fingerprint is derived from the deepest exception cause, its originating
class, and method. It is deterministic across line-number changes and differing
exception messages.

## Requirements

- Java 8 or newer
- Maven 3.6 or newer

## Build

```shell
mvn test
```

## Maven coordinates

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/arnabnandy7/bugdna</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

GitHub Packages requires authentication when downloading Maven packages,
including public packages. Configure a GitHub personal access token (classic)
with `read:packages` in your Maven `settings.xml`.

## Publishing

The GitHub Actions workflow in `.github/workflows/publish.yml` tests and
publishes the artifact when a GitHub release is created. It uses the
repository's automatically generated `GITHUB_TOKEN`; no repository secret is
required.

Before creating a stable release, remove `-SNAPSHOT` from the project version:

```shell
mvn versions:set -DnewVersion=0.1.0 -DgenerateBackupPoms=false
```

Commit and push the version change, create a matching `v0.1.0` tag, and create
a GitHub release from that tag.
