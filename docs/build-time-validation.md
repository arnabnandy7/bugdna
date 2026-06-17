# Build-Time Validation

BugDNA includes a build-time source scanner for exception-handling hazards that
often become production failures but are separate from runtime fingerprinting.

## Maven

Run the scanner directly:

```bash
mvn bugdna:scan
```

Or bind it to verification:

```xml
<plugin>
    <groupId>io.github.arnabnandy7</groupId>
    <artifactId>bugdna-maven-plugin</artifactId>
    <version>1.1.0</version>
    <executions>
        <execution>
            <goals>
                <goal>scan</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Options:

| Property | Default | Description |
| --- | --- | --- |
| `bugdna.failOnIssues` | `true` | Fail the build when findings are present. |
| `bugdna.includeTests` | `false` | Include test source roots. |
| `bugdna.skip` | `false` | Skip the scan. |

## Gradle

Apply the plugin:

```groovy
plugins {
    id 'io.github.arnabnandy7.bugdna' version '1.1.0'
}

bugdna {
    failOnIssues = true
    includeTests = false
}
```

Run the scanner:

```bash
./gradlew bugdnaScan
```

## Rules

The scanner currently reports:

- Empty catch blocks
- Generic `Exception`, `Throwable`, or `RuntimeException` catch/throw/throws usage
- Likely unhandled checked-exception APIs outside `try` blocks or methods with `throws`

The unhandled-exception rule is intentionally heuristic. Java compilation remains
the source of truth for exact checked-exception enforcement.
