# BugDNA Specification

**Version:** 1.1.2

This is the canonical reference document that TypeScript, Python, and Java implementations must follow exactly for BugDNA.

## Table of Contents
1. [Fingerprint Identity Algorithm](#1-fingerprint-identity-algorithm)
2. [Root-Cause Discovery](#2-root-cause-discovery)
3. [Signature Derivation](#3-signature-derivation)
4. [Failure Chain](#4-failure-chain)
5. [Cause Chain](#5-cause-chain)
6. [Stability Score](#6-stability-score)
7. [Category Classification](#7-category-classification)
8. [Family Classification](#8-family-classification)
9. [Priority Thresholds](#9-priority-thresholds)
10. [Similarity Scoring](#10-similarity-scoring)
11. [Diff Classification](#11-diff-classification)
12. [Drift Detection](#12-drift-detection)
13. [PII Normalization](#13-pii-normalization)
14. [Timeline & Burst Detection](#14-timeline--burst-detection)
15. [Fingerprint JSON Schema](#15-fingerprint-json-schema)

---

## 1. Fingerprint Identity Algorithm

The canonical value is constructed as:
```text
CANONICAL = rootCauseFQCN + "|" + frame1Class#frame1Method + "|" + frame2Class#frame2Method + ...
```

**Rules:**
- Take up to `MAX_FINGERPRINT_FRAMES = 5` frames from the root cause's stack trace.
- Each frame is formatted as `className + "#" + methodName`.
- If root cause has NO stack trace: use a single fallback frame containing the root cause FQCN.
  - Result: `canonical = rootCauseFQCN + "|" + rootCauseFQCN`
- The ID is: `"BUGDNA-" + SHA256(canonical as UTF-8)[0:16].toUpperCase()`
- `HASH_LENGTH = 16`, `ID_PREFIX = "BUGDNA-"`

**Exclusions from hash:** Exception messages, line numbers, timestamps, request IDs, user IDs.
**Inclusions in hash:** Deepest root-cause FQCN, up to 5 frame `class#method` strings.

## 2. Root-Cause Discovery

- Walk the exception cause chain (Java: `getCause()`, Python: `__cause__` then `__context__`, JS: `Error.cause`).
- Use an identity-based set to detect cycles (Java: `IdentityHashMap`, JS: `Set` with `===`, Python: `id()` set).
- Stop when cause is null or cycle detected.
- Return the deepest non-cyclic exception.

## 3. Signature Derivation

**If root cause has empty stack trace:**
- `signature` = simple class name of root cause (e.g., `"NullPointerException"`)
- `qualifiedSignature` = FQCN of root cause (e.g., `"java.lang.NullPointerException"`)

**If root cause has frames:**
- `Origin` = first frame (`stackTrace[0]`)
- `signature` = `simpleClassName(origin.className) + "#" + origin.methodName`
  - Nested class `$Inner` is normalized to `.Inner`.
- `qualifiedSignature` = `origin.className + "#" + origin.methodName`

**Simple class name extraction:** Take everything after the last `.` in FQCN.

## 4. Failure Chain

- Take the normalized frames list, iterate in reverse (from last to first).
- Extract simple class name from each frame's `className`.
- Deduplicate adjacent identical class names.
- Result is the architectural call flow (e.g., `["Controller", "Service", "Repository"]`).

## 5. Cause Chain

- Walk from outermost exception down through cause chain to root cause.
- Collect each exception's FQCN.
- Use identity set for cycle protection.

## 6. Stability Score

**Formula:** `min(86 + (min(stackDepth, 5) * 4), 98)`, fallback `70` when depth is 0.

| Stack Depth | Score |
|-------------|-------|
| 0           | 70    |
| 1           | 90    |
| 2           | 94    |
| 3+          | 98 (capped) |

## 7. Category Classification

Match rootCause FQCN `toLowerCase()` against substring patterns. First match wins, in this evaluation order:

| Category | Patterns |
|----------|----------|
| DATABASE | `java.sql.`, `.sql`, `database`, `jdbc`, `datasource` |
| NETWORK | `java.net.`, `socket`, `connect`, `network`, `timeout`, `http` |
| VALIDATION | `validation`, `constraint`, `illegalargument`, `parse`, `format` |
| SECURITY | `security`, `accessdenied`, `authentication`, `authorization`, `permission`, `certificate`, `ssl`, `crypto` |
| SERIALIZATION | `serialization`, `deserialization`, `invalidclass`, `invalidobject`, `notserializable`, `objectstream`, `streamcorrupted`, `json`, `xml`, `mapping`, `codec`, `decode`, `encode` |
| CONFIGURATION | `configuration`, `config`, `property`, `environment`, `missingresource` |
| BUSINESS | `business`, `domain`, `rule`, `policy` |
| UNKNOWN | fallback |

> **Note for JS/Python:** Keep Java-specific prefixes for polyglot interop, add language-native equivalents.

## 8. Family Classification

Build a unified lowercase evidence string from:
1. Root cause FQCN
2. All cause chain FQCNs
3. All normalized messages (PII-masked: numbers→`{NUMBER}`, emails→`{EMAIL}`)
4. All normalized frame strings (`className#methodName`)

**Connectivity patterns:** `connectexception`, `socketexception`, `sockettimeoutexception`, `sqltransientconnectionexception`, `sqlnontransientconnectionexception`, `sqlrecoverableexception`, `sqltimeoutexception`, `jdbcconnectionexception`, `connection refused`, `connection reset`, `connection timed out`, `socket timeout`, `communications link failure`, `unable to acquire connection`, `could not open connection`, `pool exhausted`, `connection pool`, `timeout`

**Database context patterns:** `java.sql.`, `.sql`, `database`, `jdbc`, `datasource`, `hikari`, `connectionpool`, `poolbase`, `postgres`, `mysql`, `mariadb`, `oracle`

**Classification precedence:**
1. connectivity AND (category==DATABASE OR databaseContext) → `DATABASE_CONNECTIVITY`
2. category==DATABASE → `DATABASE_OPERATION`
3. category==NETWORK → `NETWORK_CONNECTIVITY`
4. category==VALIDATION → `VALIDATION`
5. category==SECURITY → `SECURITY`
6. category==SERIALIZATION → `SERIALIZATION`
7. category==CONFIGURATION → `CONFIGURATION`
8. category==BUSINESS → `BUSINESS`
9. else → `UNKNOWN`

## 9. Priority Thresholds

Determined from `FailureContext(occurrences, affectedUsers, fatal)`:

| Priority | Condition |
|----------|----------|
| UNKNOWN | `!hasImpactData` (`occurrences < 0 OR affectedUsers < 0`) |
| CRITICAL | `fatal==true` OR `affectedUsers >= 100` OR `occurrences >= 1000` |
| HIGH | `affectedUsers >= 10` OR `occurrences >= 100` |
| MEDIUM | `affectedUsers > 0` OR `occurrences >= 10` |
| LOW | everything else with valid data |

## 10. Similarity Scoring

If IDs are identical → 100% (short circuit).

Otherwise, 5 weighted components:

| Component | Weight | Method |
|-----------|--------|--------|
| Root Cause | 35% | Exact string match (35 or 0) |
| Origin Class | 25% | Exact string match (25 or 0) |
| Method Name | 20% | `round(20 * methodSimilarity)` |
| Frames | 15% | `round(15 * frameSimilarity)` |
| Cause Chain | 5% | `round(5 * setOverlap)` |

**methodSimilarity(a, b):**
- Exact match → 1.0
- Either empty → 0.0
- One starts with the other → 0.67
- Otherwise: tokenize on camelCase boundaries (insert space before uppercase after lowercase), dashes, underscores, whitespace → lowercase tokens. Compute Jaccard: `|intersection| / |union|`

**frameSimilarity(framesA, framesB):**
- For each frame in A, find max `singleFrameSimilarity` across all frames in B.
- Sum of best scores / `max(|A|, |B|)`
- `singleFrameSimilarity`: exact match → 1.0; different class → 0.0; same class → `0.7 + 0.3 * methodSimilarity`

**setOverlap(a, b):** Jaccard index `|intersection| / |union|`

**isLikelyRelated:** percentage >= 80

## 11. Diff Classification

Strict precedence:
1. Same ID → "No Fingerprint Change"
2. Different origin class, same layer → "{Layer} Layer Changed"
3. Different layer → "Layer Changed"
4. Same class, different method → "Method Changed"
5. Same origin, different root cause → "Root Cause Changed"
6. Fallback → "Call Path Changed"

**Layer detection** (check class name `endsWith`):
- `Controller`, `Service`, `Repository`, `Gateway`, `Client`, `Handler`, `Validator`, `Configuration`/`Config`, `Mapper`, `Codec`
- Fallback: `Application`

## 12. Drift Detection

Requires: `old.id == new.id`

**Weights:** Origin Class 27%, Method 27%, Frames 46%
- `classScore` = exact match ? 27 : 0
- `methodScore` = `round(27 * methodSimilarity)`
- `frameScore` = `round(46 * frameSimilarity)`
- `driftPercentage` = 100 - (`classScore` + `methodScore` + `frameScore`)
- `isPossibleCodePathChange` = `driftPercentage >= 50`

## 13. PII Normalization

- **Numbers:** regex `\b\d+\b` → `{NUMBER}`
- **Emails:** regex `\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b` (case insensitive) → `{EMAIL}`

## 14. Timeline & Burst Detection

- **Default timeline limit:** 10,000 events (FIFO eviction when full)
- **Default burst idle gap:** 60 seconds
- **Minute bucket:** `floor(epochSecond / 60)`
- A burst for a given fingerprint ID starts with the first occurrence and continues as long as subsequent occurrences are within `maximumIdleGap` of the previous one
- **Peak rate** = max occurrences in any single minute bucket within a burst
- **Qualified burst:** `peakRatePerMinute >= minimumPeakRatePerMinute`
- Sort bursts by `peakRatePerMinute DESC`, then `id ASC`

**Default constants:**
- `DEFAULT_TOP_FAILURE_LIMIT = 10`
- `DEFAULT_TOP_FAMILY_LIMIT = 10`
- `DEFAULT_TIMELINE_LIMIT = 10_000`
- `DEFAULT_BURST_MAX_IDLE_GAP = 60 seconds`

## 15. Fingerprint JSON Schema

Serialized fingerprint format:
```json
{
  "id": "BUGDNA-A1B2C3D4E5F6A7B8",
  "rootCause": "java.lang.NullPointerException",
  "signature": "UserService#getUser",
  "qualifiedSignature": "com.example.UserService#getUser",
  "frames": ["com.example.UserService#getUser", "com.example.Controller#handle"],
  "failureChain": ["Controller", "UserService"],
  "causeChain": ["java.lang.RuntimeException", "java.lang.NullPointerException"],
  "stabilityScore": 94,
  "priority": "MEDIUM",
  "category": "UNKNOWN",
  "family": "UNKNOWN"
}
```
