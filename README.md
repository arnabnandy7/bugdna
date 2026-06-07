# bugdna

## Installation

GitHub Packages requires authentication to download Maven packages. Create a
GitHub personal access token (classic) with the `read:packages` permission, then
add it to `~/.m2/settings.xml`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

Add the GitHub Packages repository and bugdna dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/arnabnandy7/bugdna</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.arnabnandy7</groupId>
        <artifactId>bugdna</artifactId>
        <version>0.1.0-SNAPSHOT</version>
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
