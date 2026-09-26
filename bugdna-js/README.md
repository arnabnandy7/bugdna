# bugdna (Node.js / TypeScript)

Deterministic exception fingerprinting, similarity comparison, regression detection, and in-memory failure tracking for Node.js and TypeScript.

## Installation

```bash
npm install bugdna
```

## Quick Start

```typescript
import { generate, FailureTracker, compareFingerprints, diffErrors } from 'bugdna';

try {
  // application logic
} catch (err) {
  const fingerprint = generate(err as Error);
  console.log(fingerprint.id);                 // BUGDNA-...
  console.log(fingerprint.signature);          // UserService#getUser
  console.log(fingerprint.category);           // DATABASE | NETWORK | VALIDATION | ...
  console.log(fingerprint.family);             // DATABASE_CONNECTIVITY | ...
  console.log(fingerprint.stabilityScore);     // 70..98
}
```

## Features

- **Zero runtime dependencies** — uses only Node.js built-in `node:crypto`, `node:fs`, `node:path`.
- **Deterministic IDs** — excludes exception messages and line numbers from hashes so nearby source edits do not split failure groups.
- **Causal Chain Support** — walks `Error.cause` chains with cycle protection to fingerprint the deepest root cause.
- **Similarity & Diffs** — `compareFingerprints(a, b)` and `diffErrors(oldErr, newErr)`.
- **Failure Tracking** — `FailureTracker` with bounded timelines and per-minute burst detection.
- **Deployment Regression Detection** — `RegressionDetector.compare(oldSnapshot, newSnapshot)` and `detectDrift(oldFp, newFp)`.
