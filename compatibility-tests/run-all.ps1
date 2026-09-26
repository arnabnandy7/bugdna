$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot
Push-Location $RootDir

try {
    Write-Host "=== [1/4] Verifying Ecosystem Version Alignment ==="
    python compatibility-tests/check-versions.py
    if ($LASTEXITCODE -ne 0) { throw "Version alignment check failed" }

    Write-Host "=== [2/4] Running Java Specification Compatibility Tests ==="
    mvn --batch-mode --no-transfer-progress -pl bugdna-core -Dtest=SpecificationCompatibilityTest test
    if ($LASTEXITCODE -ne 0) { throw "Java compatibility tests failed" }

    Write-Host "=== [3/4] Running TypeScript Specification Compatibility Tests ==="
    npm --prefix bugdna-js run test:compat
    if ($LASTEXITCODE -ne 0) { throw "TypeScript compatibility tests failed" }

    Write-Host "=== [4/4] Running Python Specification Compatibility Tests ==="
    $env:PYTEST_DISABLE_PLUGIN_AUTOLOAD = "1"
    python -m pytest bugdna-python/tests/test_compatibility.py -v
    if ($LASTEXITCODE -ne 0) { throw "Python compatibility tests failed" }

    Write-Host "=== All Cross-Language Specification Compatibility Tests Passed! ==="
} finally {
    Pop-Location
}
