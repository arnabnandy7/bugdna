#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== [1/4] Verifying Ecosystem Version Alignment ==="
python3 compatibility-tests/check-versions.py

echo "=== [2/4] Running Java Specification Compatibility Tests ==="
mvn --batch-mode --no-transfer-progress -pl bugdna-core -Dtest=SpecificationCompatibilityTest test

echo "=== [3/4] Running TypeScript Specification Compatibility Tests ==="
npm --prefix bugdna-js run test:compat

echo "=== [4/4] Running Python Specification Compatibility Tests ==="
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest bugdna-python/tests/test_compatibility.py -v

echo "=== All Cross-Language Specification Compatibility Tests Passed! ==="
