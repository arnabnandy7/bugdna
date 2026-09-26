# bugdna (Node.js / TypeScript)

Deterministic exception fingerprinting, similarity comparison, regression detection, and in-memory failure tracking for Node.js and TypeScript.

## Installation

```bash
npm install bugdna
```

## Quick Start

```typescript
import {
  generate,
  dependencyGraph,
  lookup,
  normalize,
  BugDnaAssertions,
  FailureCategory,
  FailureTracker,
  compareFingerprints,
  diffErrors,
} from 'bugdna';

try {
  // application logic
} catch (err) {
  const fingerprint = generate(err as Error);
  console.log(fingerprint.id);                 // BUGDNA-...
  console.log(fingerprint.signature);          // UserService#getUser
  console.log(fingerprint.category);           // DATABASE | NETWORK | VALIDATION | ...
  console.log(fingerprint.family);             // DATABASE_CONNECTIVITY | ...
  console.log(fingerprint.stabilityScore);     // 70..98
  console.log(fingerprint.explain());          // multi-line summary
}
```

## Features

- **Zero runtime dependencies** — uses only Node.js built-in `node:crypto`, `node:fs`, `node:path` (dual ESM and CommonJS with TypeScript `.d.ts` types).
- **Deterministic IDs** — excludes exception messages and line numbers from hashes so nearby source edits do not split failure groups.
- **Causal Chain & Dependency Graphs** — walks `Error.cause` chains with cycle protection (`generate` and `dependencyGraph`).
- **PII-Safe Normalization** — `normalize(text)` replaces numeric tokens with `{NUMBER}` and emails with `{EMAIL}`.
- **Knowledge Base Lookup** — `lookup(id)` and `loadKnowledgeBase(source)` map `BUGDNA-*` IDs to owners, titles, and runbooks via `bugdna.yml` or `BUGDNA_KNOWLEDGE_PATH`.
- **Fluent Test Assertions** — `BugDnaAssertions.assertThat(fingerprint)` for Vitest, Jest, or Node test runners.
- **Similarity & Diffs** — `compareFingerprints(a, b)` and `diffErrors(oldErr, newErr)`.
- **Failure Tracking** — `FailureTracker` with root-cause family clustering (`families()`, `familyReport()`), bounded timelines, and per-minute burst detection (`bursts()`).
- **Batch & Consumer Tracking** — `SkipReasonAnalyzer` and topic/partition/offset-aware `ConsumerFailureTracker`.
- **Deployment Regression & Drift Detection** — `RegressionDetector.compare(oldSnapshot, newSnapshot)` and `detectDrift(oldFp, newFp)`.
