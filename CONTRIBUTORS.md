# Contributing to bugdna

Contributions are welcome through issues and pull requests.

## Development setup

You need:

- JDK 8 or newer (JDK 17+ to build `bugdna-spring-boot-starter`)
- Maven 3.6 or newer
- Node.js 18 or newer (for `bugdna-js`)
- Python 3.9 or newer (for `bugdna-python`)
- Git

Clone the repository:

```shell
git clone https://github.com/arnabnandy7/bugdna.git
cd bugdna
```

Run the Java multi-module test suite:

```shell
mvn clean test
```

Run the Node.js / TypeScript, Python, and cross-language compatibility test suite:

```shell
./compatibility-tests/run-all.sh
```

Or on Windows PowerShell:

```powershell
.\compatibility-tests\run-all.ps1
```

Do not run `mvn deploy`, `npm publish`, or `twine upload` for normal development. Release workflows sign and publish artifacts to Maven Central, npm, and PyPI.

## Making changes

- Keep changes focused on one issue or feature.
- Preserve Java 8 compatibility in `bugdna-core`, Node.js 18+ compatibility in `bugdna-js`, and Python 3.9+ compatibility in `bugdna-python`.
- Add or update tests (and `specification/` fixtures when applicable) for behavior changes.
- Keep failure fingerprints deterministic across all supported languages.
- Document public APIs with Javadocs, TypeScript types, and Python type annotations.
- Avoid including generated files from `target/`, `dist/`, or build caches.

## Pull requests

Before opening a pull request:

1. Create a branch from `main`.
2. Run `mvn clean test` and the cross-language compatibility suite (`compatibility-tests/run-all.sh` or `run-all.ps1`).
3. Confirm generated files and credentials are not committed.
4. Describe the problem, the solution, and any compatibility impact.

By contributing, you agree that your contribution will be licensed under the
project's MIT License.
