# Core Library

The core module creates deterministic failure identities without Spring, SLF4J,
Micrometer, or a database.

## Fingerprint Generation

```java
Fingerprint fingerprint = BugDna.generate(exception);
```

BugDNA uses the deepest cause, exception type, and up to five normalized stack
frames. Exception messages and line numbers are excluded so changing input values
or nearby source lines does not fragment a failure group.

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
```

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

result.getPercentage();
result.isLikelyRelated();
result.getExplanation();
```

`isLikelyRelated()` returns `true` at 80 percent or higher.

## Diffs

```java
FingerprintDiff diff = BugDiff.compare(oldException, newException);

System.out.println(diff.getSummary());
System.out.println(diff.explain());
```

Diffs highlight changes such as origin class, method, root cause, repository layer,
or normalized call path.

## Error Handling

Public generation and comparison methods reject `null`. `FailureContext.of(...)`
rejects negative counts.
