# bugdna

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
        <version>0.1.2</version>
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
