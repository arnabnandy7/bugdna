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
<dependency>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```
