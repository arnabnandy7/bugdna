# bugdna (Python)

Deterministic exception fingerprinting, similarity comparison, regression detection, and thread-safe failure tracking for Python.

## Installation

```bash
pip install bugdna
```

## Quick Start

```python
from bugdna import generate, FailureTracker, compare_fingerprints, diff_exceptions

try:
    # application logic
    raise ValueError("Invalid account id 12345")
except Exception as exc:
    fp = generate(exc)
    print(fp.id)                 # BUGDNA-...
    print(fp.signature)          # module#func or ClassName#method
    print(fp.category)           # FailureCategory.VALIDATION
    print(fp.family)             # FailureFamily.VALIDATION
    print(fp.stability_score)    # 70..98
```

## Features

- **Zero runtime dependencies** — pure Python standard library (`hashlib`, `traceback`, `dataclasses`, `threading`).
- **Deterministic IDs** — excludes exception messages and line numbers from hashes so nearby source edits do not split failure groups.
- **Causal Chain Support** — walks `__cause__` and `__context__` chains with cycle protection to fingerprint the deepest root cause.
- **Similarity & Diffs** — `compare_fingerprints(a, b)` and `diff_exceptions(old_exc, new_exc)`.
- **Failure Tracking** — thread-safe `FailureTracker` with bounded timelines and per-minute burst detection.
- **Deployment Regression Detection** — `RegressionDetector.compare(old_snapshot, new_snapshot)` and `detect_drift(old_fp, new_fp)`.
