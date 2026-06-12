## Summary

<!-- Describe the problem and how this pull request solves it. -->

## Related Issue

<!-- Link an issue with "Closes #123", or write "None". -->

## Affected Modules

<!-- Check all that apply. -->

- [ ] Core library
- [ ] Spring Boot starter
- [ ] CLI
- [ ] Documentation
- [ ] Build or CI

## Changes

<!-- List the main implementation and behavior changes. -->

-

## Testing

<!-- Describe the tests run and include any relevant output or manual checks. -->

```text
mvn clean test
```

## Compatibility and Behavior

<!-- Note API, configuration, fingerprint, or runtime compatibility impacts. -->

- Breaking change: <!-- Yes/No; explain if yes. -->
- Fingerprint output changed: <!-- Yes/No; explain if yes. -->

## Checklist

- [ ] My changes are focused on one issue or feature.
- [ ] I added or updated tests for behavior changes.
- [ ] The full test suite passes with `mvn clean test`.
- [ ] Core library changes remain compatible with Java 8.
- [ ] Failure fingerprints remain deterministic, or the change is documented above.
- [ ] Public APIs have appropriate Javadocs.
- [ ] User-facing changes are reflected in the documentation and `CHANGELOG.md`.
- [ ] I did not commit credentials or generated files from `target/`.
