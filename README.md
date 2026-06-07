# bugdna

## Installation

bugdna is available from
[Maven Central](https://central.sonatype.com/artifact/io.github.arnabnandy7/bugdna/overview).
Add the dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.arnabnandy7</groupId>
        <artifactId>bugdna</artifactId>
        <version>0.1.1</version>
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
}
```
