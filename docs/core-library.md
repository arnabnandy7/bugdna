# Core Library

The core module creates deterministic failure identities without Spring, SLF4J,
Micrometer, or a database.

See [Core vs Spring Boot starter](core-vs-starter.md) for a feature-by-feature
comparison of availability, bean registration, and automatic behavior.

## Fingerprint Generation

```java
try {
    userRepository.findById(userId);
} catch (RuntimeException failure) {
    Fingerprint fingerprint = BugDna.generate(failure);
    System.out.println(fingerprint.getId());
    System.out.println(fingerprint.getSignature());
}
```

BugDNA uses the deepest cause, exception type, and up to five normalized stack
frames. Exception messages and line numbers are excluded so changing input values
or nearby source lines does not fragment a failure group.

For example, failures containing `"user 17"` and `"user 42"` receive the same ID
when they have the same exception type and normalized call path. Changing
`UserService#getUser` to `UserService#loadUser` can produce a new ID.

## Fingerprint Data

| Method | Description |
| --- | --- |
| `getId()` | Stable `BUGDNA-*` identifier |
| `getRootCause()` | Fully qualified deepest exception type |
| `getSignature()` | Simple origin in `Class#method` form |
| `getQualifiedSignature()` | Fully qualified origin |
| `getFrames()` | Normalized frames used for grouping |
| `getFailureChain()` | Simplified application call chain |
| `getCauseChain()` | Outer-to-inner exception types |
| `getExplanation()` | Detailed grouping explanation |
| `getStabilityScore()` | Stability confidence from 0 to 100 |
| `getPriority()` | Impact-based priority |
| `getCategory()` | Broad failure category |
| `explain()` | Compact multi-line report |

## Priority Context

Without operational context, priority is `UNKNOWN`.

```java
FailureContext context = FailureContext.of(
        125,
        18,
        false
);

Fingerprint fingerprint = BugDna.generate(exception, context);
System.out.println(fingerprint.getPriority());
```

The example prints `HIGH`: 125 occurrences or 18 affected users independently meet
the high-priority threshold.

Priority thresholds:

| Priority | Condition |
| --- | --- |
| `CRITICAL` | Fatal, at least 100 affected users, or at least 1000 occurrences |
| `HIGH` | At least 10 affected users or at least 100 occurrences |
| `MEDIUM` | At least one affected user or at least 10 occurrences |
| `LOW` | Context supplied below the medium thresholds |
| `UNKNOWN` | No context supplied |

## Categories

BugDNA classifies root-cause exception names into:

- `DATABASE`
- `NETWORK`
- `VALIDATION`
- `SECURITY`
- `SERIALIZATION`
- `CONFIGURATION`
- `BUSINESS`
- `UNKNOWN`

Classification is heuristic and should be treated as operational metadata, not a
replacement for domain-specific exception handling.

## Similarity

```java
Similarity result = BugSimilarity.compare(first, second);

System.out.println(result.getPercentage());
System.out.println(result.isLikelyRelated());
System.out.println(result.getExplanation());
```

`isLikelyRelated()` returns `true` at 80 percent or higher.

Use similarity when IDs differ but a refactor may have moved or renamed the same
failure:

```java
if (result.isLikelyRelated()) {
    System.out.println("Review as one failure family");
}
```

## Diffs

```java
FingerprintDiff diff = BugDiff.compare(oldException, newException);

System.out.println(diff.getSummary());
System.out.println(diff.explain());
```

Diffs highlight changes such as origin class, method, root cause, repository layer,
or normalized call path.

Example:

```text
Method Changed

Old:
getUser

New:
loadUser
```

## Error Handling

Public generation and comparison methods reject `null`. `FailureContext.of(...)`
rejects negative counts.
