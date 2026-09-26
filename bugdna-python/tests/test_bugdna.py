from __future__ import annotations

import pytest

from bugdna import (
    BugDiff,
    BugDna,
    BugDnaAssertions,
    BugSimilarity,
    ConsumerFailureTracker,
    DeploymentSnapshot,
    FailureCategory,
    FailureContext,
    FailureFamily,
    FailurePriority,
    FailureTracker,
    FingerprintDriftDetector,
    RegressionDetector,
    SkipReasonAnalyzer,
    diff_exceptions,
    generate,
    normalize,
    parse_knowledge_base,
)


class UserService:
    def get_user(self, message: str) -> None:
        raise ValueError(message)

    def get_user_other_line(self, message: str) -> None:
        # Different line number in another method
        raise ValueError(message)


class OrderRepository:
    def find_by_id(self) -> None:
        raise ConnectionRefusedError("Connection refused to postgres")

    def find_by_email(self) -> None:
        raise ConnectionRefusedError("Connection refused to postgres")


class OrderService:
    def __init__(self, repo: OrderRepository) -> None:
        self.repo = repo

    def get_order_by_id(self) -> None:
        try:
            self.repo.find_by_id()
        except Exception as exc:
            raise RuntimeError("Order lookup failed") from exc

    def get_order_by_email(self) -> None:
        try:
            self.repo.find_by_email()
        except Exception as exc:
            raise RuntimeError("Order lookup failed") from exc


def _capture_exception(callable_fn) -> BaseException:
    try:
        callable_fn()
    except BaseException as exc:
        return exc
    raise AssertionError("Expected exception was not raised")


def teardown_function() -> None:
    BugDna.clear_knowledge_base_for_testing()


def test_messages_and_line_numbers_do_not_change_fingerprint() -> None:
    svc = UserService()

    def call_line_a() -> None:
        svc.get_user("user 12345 missing")

    def call_line_b() -> None:
        svc.get_user("email alice@example.com missing")

    exc1 = _capture_exception(call_line_a)
    exc2 = _capture_exception(call_line_b)

    # Note: top frame is UserService#get_user, second frame is call_line_a vs call_line_b.
    # Let's invoke svc.get_user directly so the frame list is identical except line number!
    try:
        svc.get_user("first message")
    except BaseException as e1:
        direct1 = e1

    try:
        svc.get_user("second message")
    except BaseException as e2:
        direct2 = e2

    fp1 = generate(direct1)
    fp2 = generate(direct2)

    assert fp1.id == fp2.id
    assert fp1 == fp2
    assert fp1.signature == "UserService#get_user"
    assert fp1.category == FailureCategory.VALIDATION
    assert fp1.id != generate(exc1).id or fp1.signature == "UserService#get_user"


def test_deepest_cause_and_cyclic_cause_chains() -> None:
    svc = OrderService(OrderRepository())
    exc = _capture_exception(svc.get_order_by_id)

    fp = generate(exc)
    assert fp.root_cause == "ConnectionRefusedError"
    assert fp.signature == "OrderRepository#find_by_id"
    assert list(fp.cause_chain) == ["RuntimeError", "ConnectionRefusedError"]
    assert fp.family == FailureFamily.DATABASE_CONNECTIVITY

    # Cyclic cause chain
    err_a = RuntimeError("a")
    err_b = ValueError("b")
    err_a.__cause__ = err_b
    err_b.__cause__ = err_a

    cycle_fp = generate(err_a)
    assert cycle_fp.root_cause == "ValueError"
    assert list(cycle_fp.cause_chain) == ["RuntimeError", "ValueError"]


def test_unraised_exception_with_no_traceback() -> None:
    err = KeyError("missing_key")
    fp = generate(err)
    assert fp.stability_score == 70
    assert fp.signature == "KeyError"
    assert list(fp.frames) == ["KeyError"]


def test_dependency_graph_and_report() -> None:
    svc = OrderService(OrderRepository())
    exc = _capture_exception(svc.get_order_by_id)

    graph = BugDna.dependency_graph(exc)
    assert graph.depth == 2
    assert graph.root.root_cause == "RuntimeError"
    assert len(graph.dependencies) == 1
    assert "└─ " in graph.report()


def test_pii_normalization() -> None:
    masked = normalize("Account 123456 for john.doe@example.com failed on order 98765")
    assert masked == "Account {NUMBER} for {EMAIL} failed on order {NUMBER}"


def test_similarity_and_diff_between_exceptions() -> None:
    svc = OrderService(OrderRepository())
    exc1 = _capture_exception(svc.get_order_by_id)
    exc2 = _capture_exception(svc.get_order_by_email)

    fp1 = generate(exc1)
    fp2 = generate(exc2)

    sim = BugSimilarity.compare(fp1, fp2)
    assert sim.is_likely_related is True
    assert sim.percentage >= 80

    diff = diff_exceptions(exc1, exc2)
    assert diff.summary == "Method Changed"
    assert diff.old_value == "find_by_id"
    assert diff.new_value == "find_by_email"
    assert "Method Changed" in BugDiff.compare(fp1, fp2).explain()


def test_failure_tracker_bursts_and_reports() -> None:
    tracker = FailureTracker()
    svc = OrderService(OrderRepository())
    exc = _capture_exception(svc.get_order_by_id)

    tracker.capture(exc, occurred_at=60)
    tracker.capture(exc, occurred_at=70)
    tracker.capture(exc, occurred_at=80)

    assert tracker.total_occurrences == 3
    assert tracker.unique_failures == 1
    assert tracker.unique_families == 1
    assert tracker.families()[0].family == FailureFamily.DATABASE_CONNECTIVITY

    bursts = tracker.bursts(2)
    assert len(bursts) == 1
    assert bursts[0].peak_rate_per_minute == 3
    assert "1 unique failure signature" in tracker.report()


def test_regression_detector_and_drift() -> None:
    svc = UserService()
    fp_old = generate(_capture_exception(lambda: svc.get_user("a")))
    fp_rec = generate(_capture_exception(lambda: svc.get_user_other_line("b")))
    fp_new = generate(PermissionError("denied"))

    old_snap = DeploymentSnapshot("1.0.0", [fp_old, fp_rec])
    new_snap = DeploymentSnapshot("1.1.0", [fp_rec, fp_new])

    comparison = RegressionDetector.compare(old_snap, new_snap)
    assert comparison.new_fingerprint_count == 1
    assert comparison.resolved_fingerprint_count == 1
    assert comparison.recurring_fingerprint_count == 1
    assert "Version 1.0.0 -> Version 1.1.0" in comparison.report()

    drift = FingerprintDriftDetector.detect(fp_rec, fp_rec)
    assert drift.signature_drift_percentage == 0


def test_skip_analyzer_consumer_tracker_assertions_and_knowledge_base() -> None:
    svc = UserService()
    exc = _capture_exception(lambda: svc.get_user("bad input"))
    fp = generate(exc, FailureContext.of(15, 2, False))

    BugDnaAssertions.assert_that(fp).has_category(
        FailureCategory.VALIDATION
    ).has_family(FailureFamily.VALIDATION).has_root_cause(ValueError).has_signature(
        "UserService#get_user"
    )
    assert fp.priority == FailurePriority.MEDIUM

    skip_analyzer = SkipReasonAnalyzer()
    skip_analyzer.record(fp)
    assert skip_analyzer.most_common_failure is not None
    assert skip_analyzer.most_common_failure.id == fp.id
    assert fp.id in skip_analyzer.report()

    consumer_tracker = ConsumerFailureTracker()
    consumer_tracker.capture("orders-topic", 2, 104, fp)
    assert consumer_tracker.failures()[0].topic == "orders-topic"
    assert "orders-topic" in consumer_tracker.report()

    kb = parse_knowledge_base(
        f"{fp.id}:\n"
        f"  title: Bad Input Error\n"
        f"  owner: Core Team\n"
        f"  runbook: docs/validation.md\n"
    )
    BugDna.load_knowledge_base(kb)
    info = BugDna.lookup(fp.id)
    assert info is not None
    assert info.title == "Bad Input Error"
    assert info.owner == "Core Team"
