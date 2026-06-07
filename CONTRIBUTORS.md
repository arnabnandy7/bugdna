# Contributing to bugdna

Contributions are welcome through issues and pull requests.

## Development setup

You need:

- JDK 8 or newer
- Maven 3.6 or newer
- Git

Clone the repository:

```shell
git clone https://github.com/arnabnandy7/bugdna.git
cd bugdna
```

Run the test suite:

```shell
mvn clean test
```

Do not run `mvn deploy` for normal development. The deploy lifecycle signs and
publishes release artifacts to Maven Central.

## Making changes

- Keep changes focused on one issue or feature.
- Preserve compatibility with Java 8.
- Add or update tests for behavior changes.
- Keep failure fingerprints deterministic.
- Document public APIs with Javadocs.
- Avoid including generated files from `target/`.

## Pull requests

Before opening a pull request:

1. Create a branch from `main`.
2. Run `mvn clean test`.
3. Confirm generated files and credentials are not committed.
4. Describe the problem, the solution, and any compatibility impact.

By contributing, you agree that your contribution will be licensed under the
project's MIT License.
