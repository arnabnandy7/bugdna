# Core Library

The core library creates deterministic failure identities without external runtime dependencies, logging frameworks, or databases. It is implemented natively for **Java** (`bugdna-core`), **Node.js / TypeScript** (`bugdna` on npm), and **Python** (`bugdna` on PyPI), following the shared [BugDNA Specification](specification.md).

See [Core vs Spring Boot starter](core-vs-starter.md) for a feature-by-feature comparison of core availability versus the Java Spring Boot starter.

## Fingerprint Generation

### Java

```java
try {
    userRepository.findById(userId);
} catch (RuntimeException failure) {
    Fingerprint fingerprint = BugDna.generate(failure);
    System.out.println(fingerprint.getId());
    System.out.println(fingerprint.getSignature());
}
```

### Node.js / TypeScript

```typescript
import { generate } from 'bugdna';

try {
  userRepository.findById(userId);
} catch (failure) {
  const fingerprint = generate(failure as Error);
  console.log(fingerprint.id);
  console.log(fingerprint.signature);
}
```

### Python

```python
from bugdna import generate

try:
    user_repository.find_by_id(user_id)
except Exception as failure:
    fingerprint = generate(failure)
    print(fingerprint.id)
    print(fingerprint.signature)
```

BugDNA uses the deepest cause (`Throwable.getCause()` in Java, `Error.cause` in Node.js, and `__cause__` / unsuppressed `__context__` in Python), the exception type, and up to five normalized stack frames. Exception messages and line numbers are excluded so changing input values or nearby source lines does not fragment a failure group.

For example, failures containing `"user 17"` and `"user 42"` receive the same ID when they have the same exception type and normalized call path. Changing `UserService#getUser` to `UserService#loadUser` can produce a new ID.

## Fingerprint Data

| Java Method | Node.js / TypeScript | Python | Description |
| --- | --- | --- | --- |
| `getId()` | `id` / `getId()` | `id` / `get_id()` | Stable `BUGDNA-*` identifier |
| `getRootCause()` | `rootCause` / `getRootCause()` | `root_cause` / `get_root_cause()` | Deepest exception type name |
| `getSignature()` | `signature` / `getSignature()` | `signature` / `get_signature()` | Simple origin in `Class#method` form |
| `getQualifiedSignature()` | `qualifiedSignature` / `getQualifiedSignature()` | `qualified_signature` / `get_qualified_signature()` | Fully qualified origin |
| `getFrames()` | `frames` / `getFrames()` | `frames` / `get_frames()` | Normalized frames used for grouping |
| `getFailureChain()` | `failureChain` / `getFailureChain()` | `failure_chain` / `get_failure_chain()` | Simplified application call chain |
| `getCauseChain()` | `causeChain` / `getCauseChain()` | `cause_chain` / `get_cause_chain()` | Outer-to-inner exception types |
| `getExplanation()` | `explanation` / `getExplanation()` | `explanation` / `get_explanation()` | Detailed grouping explanation |
| `getStabilityScore()` | `stabilityScore` / `getStabilityScore()` | `stability_score` / `get_stability_score()` | Stability confidence from 0 to 100 |
| `getPriority()` | `priority` / `getPriority()` | `priority` / `get_priority()` | Impact-based priority |
| `getCategory()` | `category` / `getCategory()` | `category` / `get_category()` | Broad failure category |
| `getFamily()` | `family` / `getFamily()` | `family` / `get_family()` | Operational root-cause family |
| `explain()` | `explain()` | `explain()` | Compact multi-line report |

*(Note: Python and TypeScript types also provide camelCase getter methods such as `getId()` and `getRootCause()` for cross-language parity.)*

## Fingerprint Knowledge Base

Use a small YAML file to turn stable fingerprint IDs into operational context:

```yaml
BUGDNA-001:
  title: Database Pool Exhaustion
  owner: Platform Team
  runbook: runbooks/db-pool.md
  dashboard: https://example.test/dashboards/db
```

Then look it up by ID:

```java
// Java
FingerprintKnowledge context = BugDna.lookup("BUGDNA-001");
System.out.println(context.getTitle());
System.out.println(context.getOwner());
System.out.println(context.getRunbook());
System.out.println(context.get("dashboard"));
```

```typescript
// Node.js / TypeScript
import { lookup } from 'bugdna';

const context = lookup('BUGDNA-001');
console.log(context?.getTitle());
console.log(context?.getOwner());
console.log(context?.getRunbook());
console.log(context?.get('dashboard'));
```

```python
# Python
from bugdna import lookup

context = lookup("BUGDNA-001")
print(context.title)
print(context.owner)
print(context.runbook)
print(context.get("dashboard"))
```

`lookup(...)` lazily reads the first available default file from the working directory: `bugdna.yml`, `bugdna.yaml`, `bugdna-fingerprints.yml`, or `bugdna-fingerprints.yaml`.
- In **Java**, set `-Dbugdna.knowledge.path=/path/to/file.yml` or call `BugDna.loadKnowledgeBase(path)`.
- In **Node.js / TypeScript**, set `BUGDNA_KNOWLEDGE_PATH=/path/to/file.yml` or call `loadKnowledgeBase(path)`.
- In **Python**, set `BUGDNA_KNOWLEDGE_PATH=/path/to/file.yml` or call `load_knowledge_base(path)`.

## Priority Context

Without operational context, priority is `UNKNOWN`.

```java
// Java
FailureContext context = FailureContext.of(125, 18, false);
Fingerprint fingerprint = BugDna.generate(exception, context);
System.out.println(fingerprint.getPriority());
```

```typescript
// Node.js / TypeScript
import { generate, FailureContext } from 'bugdna';
const fingerprint = generate(err, FailureContext.of(125, 18, false));
console.log(fingerprint.priority);
```

```python
# Python
from bugdna import generate, FailureContext
fingerprint = generate(exc, FailureContext.of(125, 18, False))
print(fingerprint.priority)
```

The example prints `HIGH`: 125 occurrences or 18 affected users independently meet the high-priority threshold.

Priority thresholds:

| Priority | Condition |
| --- | --- |
| `CRITICAL` | Fatal, at least 100 affected users, or at least 1000 occurrences |
| `HIGH` | At least 10 affected users or at least 100 occurrences |
| `MEDIUM` | At least one affected user or at least 10 occurrences |
| `LOW` | Context supplied below the medium thresholds |
| `UNKNOWN` | No context supplied |

## Categories

BugDNA classifies root-cause exception names into `FailureCategory`:

- `DATABASE`
- `NETWORK`
- `VALIDATION`
- `SECURITY`
- `SERIALIZATION`
- `CONFIGURATION`
- `BUSINESS`
- `UNKNOWN`

Classification is heuristic and should be treated as operational metadata, not a replacement for domain-specific exception handling.

## Root-Cause Families

Families (`FailureFamily`) cluster different fingerprint IDs that point to the same operational problem:

- `DATABASE_CONNECTIVITY`
- `DATABASE_OPERATION`
- `NETWORK_CONNECTIVITY`
- `VALIDATION`
- `SECURITY`
- `SERIALIZATION`
- `CONFIGURATION`
- `BUSINESS`
- `UNKNOWN`

For example, database connection refusal, socket timeout, and connection pool exhaustion can all be classified as `DATABASE_CONNECTIVITY`.

```java
Fingerprint fingerprint = BugDna.generate(failure);
System.out.println(fingerprint.getFamily());
```

Family classification uses exception types, cause names, normalized frames, and PII-masked message keywords. Messages remain excluded from the fingerprint hash, so family classification never changes `BUGDNA-*` identity.

## Similarity

```java
// Java
Similarity result = BugSimilarity.compare(first, second);
System.out.println(result.getPercentage());
System.out.println(result.isLikelyRelated());
System.out.println(result.getExplanation());
```

```typescript
// Node.js / TypeScript
import { compareFingerprints } from 'bugdna';
const result = compareFingerprints(first, second);
console.log(result.percentage, result.likelyRelated, result.explanation);
```

```python
# Python
from bugdna import compare_fingerprints
result = compare_fingerprints(first, second)
print(result.percentage, result.likely_related, result.explanation)
```

`isLikelyRelated()` / `likelyRelated` / `likely_related` returns `true` at 80 percent or higher.

## Deployment Regression Detection

Compare the unique fingerprints observed in two deployed versions:

```java
// Java
DeploymentSnapshot previous = new DeploymentSnapshot("1.2.0", previousFingerprints);
DeploymentSnapshot current = new DeploymentSnapshot("1.3.0", currentFingerprints);

DeploymentComparison comparison = RegressionDetector.compare(previous, current);
System.out.println(comparison.report());
```

(`DeploymentSnapshot` and `RegressionDetector.compare(previous, current)` work identically in Node.js / TypeScript and Python.)

```text
Version 1.2.0 -> Version 1.3.0

New fingerprints: 4
Resolved fingerprints: 12
Recurring fingerprints: 8
```

Snapshots deduplicate fingerprints by ID. A fingerprint is:

- New when it appears only in the newer deployment
- Resolved when it appears only in the older deployment
- Recurring when it appears in both deployments

Occurrence-count changes do not change these classifications. Both `RegressionDetector` and `FingerprintDriftDetector` (`detectDrift` in JS/TS, `detect_drift` in Python) also detect when a recurring fingerprint ID changes signature shape across releases.

## Diffs

```java
// Java
FingerprintDiff diff = BugDiff.compare(oldException, newException);
System.out.println(diff.getSummary());
System.out.println(diff.explain());
```

```typescript
// Node.js / TypeScript
import { diffErrors } from 'bugdna';
const diff = diffErrors(oldError, newError);
console.log(diff.summary);
console.log(diff.explain());
```

```python
# Python
from bugdna import diff_exceptions
diff = diff_exceptions(old_exc, new_exc)
print(diff.summary)
print(diff.explain())
```

Diffs highlight changes such as origin class, method, root cause, architectural layer, or normalized call path:

```text
Method Changed

Old:
getUser

New:
loadUser
```

## Error Handling

Public generation and comparison methods reject `null` / `None`. `FailureContext.of(...)` rejects negative counts.
