from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import pytest

from bugdna import (
    FailureCategory,
    FailureContext,
    FailureFamily,
    FailurePriority,
    FailureTracker,
    Fingerprint,
    categorize,
    classify_family_from_evidence,
    compare_fingerprints,
    detect_drift,
    diff_fingerprints,
    generate_from_synthetic,
    prioritize,
)

FIXTURES_DIR = Path(__file__).resolve().parents[2] / "specification" / "fixtures"


def _load_fixture(file_name: str) -> list[dict[str, Any]]:
    path = FIXTURES_DIR / file_name
    return json.loads(path.read_text(encoding="utf-8"))


def _make_dummy_fingerprint(fid: str) -> Fingerprint:
    return Fingerprint(
        id=fid,
        root_cause="java.lang.RuntimeException",
        signature="Example#run",
        qualified_signature="com.example.Example#run",
        frames=["com.example.Example#run"],
        failure_chain=["Example"],
        cause_chain=["java.lang.RuntimeException"],
        explanation="synthetic",
        stability_score=90,
        priority=FailurePriority.UNKNOWN,
        category=FailureCategory.UNKNOWN,
        family=FailureFamily.UNKNOWN,
    )


@pytest.mark.parametrize(
    "case",
    _load_fixture("identity.json"),
    ids=lambda c: c["description"],
)
def test_identity_fixtures(case: dict[str, Any]) -> None:
    fp = generate_from_synthetic(case["input"])
    expected = case["expected"]
    if "id" in expected:
        assert fp.id == expected["id"]
    if "stabilityScore" in expected:
        assert fp.stability_score == expected["stabilityScore"]
    if "signature" in expected:
        assert fp.signature == expected["signature"]
    if "qualifiedSignature" in expected:
        assert fp.qualified_signature == expected["qualifiedSignature"]
    if "failureChain" in expected:
        assert list(fp.failure_chain) == expected["failureChain"]
    if "frameCount" in expected:
        assert len(fp.frames) == expected["frameCount"]


@pytest.mark.parametrize(
    "case",
    _load_fixture("category.json"),
    ids=lambda c: c["description"],
)
def test_category_fixtures(case: dict[str, Any]) -> None:
    result = categorize(case["input"]["rootCause"])
    assert result.value == case["expected"]["category"]


@pytest.mark.parametrize(
    "case",
    _load_fixture("family.json"),
    ids=lambda c: c["description"],
)
def test_family_fixtures(case: dict[str, Any]) -> None:
    inp = case["input"]
    result = classify_family_from_evidence(inp["category"], inp["evidence"])
    assert result.value == case["expected"]["family"]


@pytest.mark.parametrize(
    "case",
    _load_fixture("priority.json"),
    ids=lambda c: c["description"],
)
def test_priority_fixtures(case: dict[str, Any]) -> None:
    ctx = FailureContext.from_input(case["input"])
    result = prioritize(ctx)
    assert result.value == case["expected"]["priority"]


@pytest.mark.parametrize(
    "case",
    _load_fixture("similarity.json"),
    ids=lambda c: c["description"],
)
def test_similarity_fixtures(case: dict[str, Any]) -> None:
    inp = case["input"]
    sim = compare_fingerprints(inp["first"], inp["second"])
    expected = case["expected"]
    if "percentage" in expected:
        assert sim.percentage == expected["percentage"]
    assert sim.is_likely_related is expected["isLikelyRelated"]


@pytest.mark.parametrize(
    "case",
    _load_fixture("diff.json"),
    ids=lambda c: c["description"],
)
def test_diff_fixtures(case: dict[str, Any]) -> None:
    inp = case["input"]
    diff = diff_fingerprints(inp["old"], inp["new"])
    assert diff.summary == case["expected"]["summary"]


@pytest.mark.parametrize(
    "case",
    _load_fixture("drift.json"),
    ids=lambda c: c["description"],
)
def test_drift_fixtures(case: dict[str, Any]) -> None:
    inp = case["input"]
    drift = detect_drift(inp["old"], inp["new"])
    expected = case["expected"]
    if "signatureDriftPercentage" in expected:
        assert drift.signature_drift_percentage == expected["signatureDriftPercentage"]
    assert drift.is_possible_code_path_change is expected["isPossibleCodePathChange"]


@pytest.mark.parametrize(
    "case",
    _load_fixture("timeline.json"),
    ids=lambda c: c["description"],
)
def test_timeline_fixtures(case: dict[str, Any]) -> None:
    inp = case["input"]
    tracker = FailureTracker(timeline_limit=inp["timelineLimit"])
    for ev in inp["events"]:
        tracker.capture_fingerprint(
            _make_dummy_fingerprint(ev["id"]),
            occurred_at=ev["epochSecond"],
        )
    timeline = tracker.timeline()
    expected = case["expected"]
    if "size" in expected:
        assert len(timeline) == expected["size"]
    assert [e.id for e in timeline] == expected["order"]


@pytest.mark.parametrize(
    "case",
    _load_fixture("burst.json"),
    ids=lambda c: c["description"],
)
def test_burst_fixtures(case: dict[str, Any]) -> None:
    inp = case["input"]
    tracker = FailureTracker()
    for ev in inp["events"]:
        tracker.capture_fingerprint(
            _make_dummy_fingerprint(ev["id"]),
            occurred_at=ev["epochSecond"],
        )
    bursts = tracker.bursts(
        minimum_peak_rate_per_minute=inp["minimumPeakRatePerMinute"],
        maximum_idle_gap=inp["maximumIdleGapSeconds"],
    )
    expected = case["expected"]
    assert len(bursts) == expected["burstCount"]
    for actual_burst, exp_burst in zip(bursts, expected["bursts"]):
        assert actual_burst.id == exp_burst["id"]
        assert actual_burst.occurrences == exp_burst["occurrences"]
        if "peakRatePerMinute" in exp_burst:
            assert actual_burst.peak_rate_per_minute == exp_burst["peakRatePerMinute"]
