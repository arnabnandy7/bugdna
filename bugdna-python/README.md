# bugdna (Python)

Deterministic exception fingerprinting, similarity comparison, regression detection, and thread-safe failure tracking for Python.

## Installation

```bash
pip install bugdna
```

## Quick Start

```python
from bugdna import (
    BugDnaAssertions,
    FailureCategory,
    FailureTracker,
    compare_fingerprints,
    dependency_graph,
    diff_exceptions,
    generate,
    lookup,
    normalize,
)

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
    print(fp.explain())          # multi-line summary
```

## Features

- **Zero runtime dependencies** — pure Python standard library (`hashlib`, `traceback`, `dataclasses`, `threading`) with `PEP 561` type annotations.
- **Deterministic IDs** — excludes exception messages and line numbers from hashes so nearby source edits do not split failure groups.
- **Causal Chain & Dependency Graphs** — walks `__cause__` and unsuppressed `__context__` chains with cycle protection (`generate` and `dependency_graph`).
- **PII-Safe Normalization** — `normalize(text)` replaces numeric tokens with `{NUMBER}` and emails with `{EMAIL}`.
- **Knowledge Base Lookup** — `lookup(id)` and `load_knowledge_base(source)` map `BUGDNA-*` IDs to owners, titles, and runbooks via `bugdna.yml` or `BUGDNA_KNOWLEDGE_PATH`.
- **Fluent Test Assertions** — `BugDnaAssertions.assert_that(fp)` for `pytest` and `unittest`.
- **Similarity & Diffs** — `compare_fingerprints(a, b)` and `diff_exceptions(old_exc, new_exc)`.
- **Failure Tracking** — thread-safe `FailureTracker` with root-cause family clustering (`families()`, `family_report()`), bounded timelines, and per-minute burst detection (`bursts()`).
- **Batch & Consumer Tracking** — `SkipReasonAnalyzer` and topic/partition/offset-aware `ConsumerFailureTracker`.
- **Deployment Regression & Drift Detection** — `RegressionDetector.compare(old_snapshot, new_snapshot)` and `detect_drift(old_fp, new_fp)`.
