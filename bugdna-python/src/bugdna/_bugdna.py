from __future__ import annotations

from dataclasses import dataclass
import hashlib
from pathlib import Path
from types import TracebackType
from typing import Any, Iterable, Mapping, Optional, Sequence, Union

from ._category import FailureCategory, categorize
from ._context import FailureContext
from ._family import (
    FailureFamily,
    build_family_evidence,
    classify_family_from_evidence,
)
from ._fingerprint import Fingerprint
from ._knowledge import (
    FingerprintKnowledge,
    clear_knowledge_base_for_testing,
    load_knowledge_base,
    lookup,
    read_knowledge_base,
)
from ._normalize import normalize
from ._priority import FailurePriority, prioritize
from ._shape import parse_signature, simple_class_name

_ID_PREFIX = "BUGDNA-"
_HASH_LENGTH = 16
_MAX_FINGERPRINT_FRAMES = 5


@dataclass(frozen=True)
class FailureDependencyGraph:
    fingerprints: tuple[Fingerprint, ...]

    def __init__(self, fingerprints: Iterable[Fingerprint]) -> None:
        if fingerprints is None:
            raise ValueError("fingerprints must not be None")
        fps = tuple(fingerprints)
        if not fps:
            raise ValueError("fingerprints must not be empty")
        object.__setattr__(self, "fingerprints", fps)

    @property
    def root(self) -> Fingerprint:
        return self.fingerprints[0]

    @property
    def dependencies(self) -> tuple[Fingerprint, ...]:
        return self.fingerprints[1:]

    @property
    def depth(self) -> int:
        return len(self.fingerprints)

    def get_root(self) -> Fingerprint:
        return self.root

    def getRoot(self) -> Fingerprint:
        return self.root

    def get_fingerprints(self) -> tuple[Fingerprint, ...]:
        return self.fingerprints

    def getFingerprints(self) -> tuple[Fingerprint, ...]:
        return self.fingerprints

    def get_dependencies(self) -> tuple[Fingerprint, ...]:
        return self.dependencies

    def getDependencies(self) -> tuple[Fingerprint, ...]:
        return self.dependencies

    def get_depth(self) -> int:
        return self.depth

    def getDepth(self) -> int:
        return self.depth

    def report(self) -> str:
        lines: list[str] = []
        for idx, fp in enumerate(self.fingerprints):
            if idx == 0:
                lines.append(fp.id)
            else:
                indent = " " + ("     " * (idx - 1))
                lines.append(f"{indent}└─ {fp.id}")
        return "\n".join(lines)

    def __str__(self) -> str:
        return f"FailureDependencyGraph{{depth={self.depth}, root='{self.root.id}'}}"


class FingerprintAssert:
    def __init__(self, fingerprint: Fingerprint) -> None:
        if fingerprint is None:
            raise AssertionError("Expected fingerprint to be non-null, but was None")
        self._fingerprint = fingerprint

    def has_category(self, expected: Union[FailureCategory, str]) -> FingerprintAssert:
        cat = (
            expected
            if isinstance(expected, FailureCategory)
            else FailureCategory(expected)
        )
        if self._fingerprint.category != cat:
            raise AssertionError(
                f"Expected category <{cat.value}> but was <{self._fingerprint.category.value}> "
                f"for fingerprint <{self._fingerprint.id}>"
            )
        return self

    def hasCategory(self, expected: Union[FailureCategory, str]) -> FingerprintAssert:
        return self.has_category(expected)

    def has_family(self, expected: Union[FailureFamily, str]) -> FingerprintAssert:
        fam = (
            expected
            if isinstance(expected, FailureFamily)
            else FailureFamily(expected)
        )
        if self._fingerprint.family != fam:
            raise AssertionError(
                f"Expected family <{fam.value}> but was <{self._fingerprint.family.value}> "
                f"for fingerprint <{self._fingerprint.id}>"
            )
        return self

    def hasFamily(self, expected: Union[FailureFamily, str]) -> FingerprintAssert:
        return self.has_family(expected)

    def has_root_cause(
        self, expected: Union[str, type[BaseException]]
    ) -> FingerprintAssert:
        if expected is None:
            raise ValueError("expected root_cause must not be None")
        expected_name = (
            expected if isinstance(expected, str) else _resolve_exception_type_name(expected)
        )
        if self._fingerprint.root_cause != expected_name:
            raise AssertionError(
                f"Expected rootCause <{expected_name}> but was <{self._fingerprint.root_cause}> "
                f"for fingerprint <{self._fingerprint.id}>"
            )
        return self

    def hasRootCause(
        self, expected: Union[str, type[BaseException]]
    ) -> FingerprintAssert:
        return self.has_root_cause(expected)

    def has_id(self, expected: str) -> FingerprintAssert:
        if self._fingerprint.id != expected:
            raise AssertionError(
                f"Expected id <{expected}> but was <{self._fingerprint.id}>"
            )
        return self

    def hasId(self, expected: str) -> FingerprintAssert:
        return self.has_id(expected)

    def has_signature(self, expected: str) -> FingerprintAssert:
        if self._fingerprint.signature != expected:
            raise AssertionError(
                f"Expected signature <{expected}> but was <{self._fingerprint.signature}> "
                f"for fingerprint <{self._fingerprint.id}>"
            )
        return self

    def hasSignature(self, expected: str) -> FingerprintAssert:
        return self.has_signature(expected)

    def has_qualified_signature(self, expected: str) -> FingerprintAssert:
        if self._fingerprint.qualified_signature != expected:
            raise AssertionError(
                f"Expected qualifiedSignature <{expected}> but was "
                f"<{self._fingerprint.qualified_signature}> for fingerprint <{self._fingerprint.id}>"
            )
        return self

    def hasQualifiedSignature(self, expected: str) -> FingerprintAssert:
        return self.has_qualified_signature(expected)

    def has_stability_score(self, expected: int) -> FingerprintAssert:
        if self._fingerprint.stability_score != expected:
            raise AssertionError(
                f"Expected stabilityScore <{expected}> but was <{self._fingerprint.stability_score}> "
                f"for fingerprint <{self._fingerprint.id}>"
            )
        return self

    def hasStabilityScore(self, expected: int) -> FingerprintAssert:
        return self.has_stability_score(expected)

    def actual(self) -> Fingerprint:
        return self._fingerprint


class BugDnaAssertions:
    @staticmethod
    def assert_that(fingerprint: Fingerprint) -> FingerprintAssert:
        return FingerprintAssert(fingerprint)

    @staticmethod
    def assertThat(fingerprint: Fingerprint) -> FingerprintAssert:
        return FingerprintAssert(fingerprint)


def _short_hash(value: str) -> str:
    digest = hashlib.sha256(value.encode("utf-8")).hexdigest()
    return digest[:_HASH_LENGTH].upper()


def _create_failure_chain(frames: Sequence[str]) -> list[str]:
    chain: list[str] = []
    for frame in reversed(frames):
        parts = parse_signature(frame)
        simple_name = simple_class_name(parts.class_name)
        if not chain or chain[-1] != simple_name:
            chain.append(simple_name)
    return chain


def _calculate_stability_score(stack_depth: int) -> int:
    if stack_depth == 0:
        return 70
    normalized_count = min(stack_depth, _MAX_FINGERPRINT_FRAMES)
    score = 86 + (normalized_count * 4)
    return min(score, 98)


def _create_explanation(
    root_cause: str,
    qualified_signature: str,
    frame_count: int,
    stability_score: int,
    cause_chain: Sequence[str],
    priority: FailurePriority,
    context: FailureContext,
) -> str:
    plural = "" if frame_count == 1 else "s"
    explanation = (
        f"{root_cause} originated at {qualified_signature} and was grouped using "
        f"{frame_count} normalized stack frame{plural}. "
        f"Fingerprint stability confidence is {stability_score}%."
    )
    if len(cause_chain) > 1:
        explanation += f" Cause chain: {' -> '.join(cause_chain)}."
    if priority == FailurePriority.UNKNOWN:
        explanation += " Priority is unknown because no impact context was supplied."
    else:
        fatal_str = "true" if context.fatal else "false"
        explanation += (
            f" Priority {priority.value} is based on {context.occurrences} occurrence(s), "
            f"{context.affected_users} affected user(s), and fatal={fatal_str}."
        )
    return explanation


def _resolve_exception_type_name(
    exc_or_cls: Union[BaseException, type[BaseException]],
) -> str:
    custom_name = getattr(exc_or_cls, "_bugdna_type_name", None)
    if isinstance(custom_name, str) and custom_name:
        return custom_name
    cls = exc_or_cls if isinstance(exc_or_cls, type) else type(exc_or_cls)
    module = getattr(cls, "__module__", "builtins")
    qualname = getattr(cls, "__qualname__", cls.__name__)
    if module in ("builtins", "__main__"):
        return qualname
    return f"{module}.{qualname}"


def _next_cause(exc: BaseException) -> Optional[BaseException]:
    if exc.__cause__ is not None:
        return exc.__cause__
    if not getattr(exc, "__suppress_context__", False) and exc.__context__ is not None:
        return exc.__context__
    return None


def _find_root_cause(failure: BaseException) -> BaseException:
    visited: set[int] = set()
    current = failure
    while True:
        visited.add(id(current))
        nxt = _next_cause(current)
        if nxt is None or id(nxt) in visited:
            break
        current = nxt
    return current


def _collect_cause_chain_and_messages(
    failure: BaseException,
) -> tuple[list[str], list[str]]:
    visited: set[int] = set()
    causes: list[str] = []
    messages: list[str] = []
    current: Optional[BaseException] = failure
    while current is not None and id(current) not in visited:
        visited.add(id(current))
        causes.append(_resolve_exception_type_name(current))
        msg = str(current)
        if msg:
            messages.append(msg)
        current = _next_cause(current)
    return causes, messages


def _extract_python_frames(exc: BaseException) -> list[str]:
    custom_frames = getattr(exc, "_bugdna_frames", None)
    if custom_frames is not None:
        return [
            f if isinstance(f, str) else f"{f['class']}#{f['method']}"
            for f in custom_frames
        ]

    tb: Optional[TracebackType] = exc.__traceback__
    if tb is None:
        return []

    raw_frames: list[str] = []
    curr_tb: Optional[TracebackType] = tb
    while curr_tb is not None:
        f = curr_tb.tb_frame
        code = f.f_code
        func_name = code.co_name
        class_name: Optional[str] = None

        # Check co_qualname (Python 3.11+)
        co_qualname = getattr(code, "co_qualname", None)
        if isinstance(co_qualname, str) and "." in co_qualname and "<locals>" not in co_qualname:
            last_dot = co_qualname.rfind(".")
            class_part = co_qualname[:last_dot]
            method_part = co_qualname[last_dot + 1 :]
            mod_name = f.f_globals.get("__name__", "")
            if mod_name and mod_name not in ("__main__", "builtins"):
                class_name = f"{mod_name}.{class_part}"
            else:
                class_name = class_part
            func_name = method_part
        elif "self" in f.f_locals and f.f_locals["self"] is not None:
            self_cls = type(f.f_locals["self"])
            mod_name = getattr(self_cls, "__module__", "")
            qual = getattr(self_cls, "__qualname__", self_cls.__name__)
            class_name = (
                f"{mod_name}.{qual}"
                if mod_name and mod_name not in ("__main__", "builtins")
                else qual
            )
        elif "cls" in f.f_locals and isinstance(f.f_locals["cls"], type):
            cls_obj = f.f_locals["cls"]
            mod_name = getattr(cls_obj, "__module__", "")
            qual = getattr(cls_obj, "__qualname__", cls_obj.__name__)
            class_name = (
                f"{mod_name}.{qual}"
                if mod_name and mod_name not in ("__main__", "builtins")
                else qual
            )
        else:
            stem = Path(code.co_filename).stem or "Anonymous"
            class_name = stem

        raw_frames.append(f"{class_name}#{func_name}")
        curr_tb = curr_tb.tb_next

    # Reverse so index 0 is the innermost frame where the exception originated
    raw_frames.reverse()
    return raw_frames


def generate_from_synthetic(
    input_data: Mapping[str, Any],
    context: Union[FailureContext, Mapping[str, Any], None] = None,
) -> Fingerprint:
    if not input_data or not input_data.get("rootCause"):
        raise ValueError("rootCause must not be empty")
    ctx = FailureContext.from_input(context)
    root_cause_name = str(input_data["rootCause"])
    raw_input_frames = input_data.get("frames") or ()
    parsed_frames: list[str] = [
        f if isinstance(f, str) else f"{f['class']}#{f['method']}"
        for f in raw_input_frames
    ]
    stack_depth = len(parsed_frames)

    if stack_depth == 0:
        signature = simple_class_name(root_cause_name)
        qualified_signature = root_cause_name
        frames = [root_cause_name]
    else:
        origin = parse_signature(parsed_frames[0])
        signature = f"{simple_class_name(origin.class_name)}#{origin.method_name}"
        qualified_signature = f"{origin.class_name}#{origin.method_name}"
        frames = parsed_frames[:_MAX_FINGERPRINT_FRAMES]

    failure_chain = _create_failure_chain(frames)
    raw_causes = input_data.get("causeChain")
    cause_chain = list(raw_causes) if raw_causes else [root_cause_name]
    canonical_value = f"{root_cause_name}|{'|'.join(frames)}"
    stability_score = _calculate_stability_score(stack_depth)
    priority = prioritize(ctx)
    category = categorize(root_cause_name)
    evidence = build_family_evidence(
        root_cause_name,
        cause_chain,
        input_data.get("messages") or (),
        frames,
    )
    family = classify_family_from_evidence(category, evidence)
    explanation = _create_explanation(
        root_cause_name,
        qualified_signature,
        len(frames),
        stability_score,
        cause_chain,
        priority,
        ctx,
    )

    return Fingerprint(
        id=_ID_PREFIX + _short_hash(canonical_value),
        root_cause=root_cause_name,
        signature=signature,
        qualified_signature=qualified_signature,
        frames=frames,
        failure_chain=failure_chain,
        cause_chain=cause_chain,
        explanation=explanation,
        stability_score=stability_score,
        priority=priority,
        category=category,
        family=family,
    )


def generate(
    failure: BaseException,
    context: Union[FailureContext, Mapping[str, Any], None] = None,
) -> Fingerprint:
    """Generates a deterministic fingerprint from a Python exception."""
    if failure is None:
        raise ValueError("failure must not be None")
    ctx = FailureContext.from_input(context)
    root_cause = _find_root_cause(failure)
    return _create_fingerprint_from_exceptions(failure, root_cause, ctx)


def _create_fingerprint_from_exceptions(
    failure: BaseException,
    root_cause: BaseException,
    ctx: FailureContext,
) -> Fingerprint:
    root_cause_name = _resolve_exception_type_name(root_cause)
    raw_frames = _extract_python_frames(root_cause)
    cause_chain, messages = _collect_cause_chain_and_messages(failure)
    return generate_from_synthetic(
        {
            "rootCause": root_cause_name,
            "frames": raw_frames,
            "causeChain": cause_chain,
            "messages": messages,
        },
        ctx,
    )


def dependency_graph(failure: BaseException) -> FailureDependencyGraph:
    """Generates a causal dependency graph from an exception chain."""
    if failure is None:
        raise ValueError("failure must not be None")
    visited: set[int] = set()
    fingerprints: list[Fingerprint] = []
    current: Optional[BaseException] = failure

    while current is not None and id(current) not in visited:
        visited.add(id(current))
        fingerprints.append(
            _create_fingerprint_from_exceptions(
                current, current, FailureContext.unknown()
            )
        )
        current = _next_cause(current)

    return FailureDependencyGraph(fingerprints)


class BugDna:
    generate = staticmethod(generate)
    generate_from_synthetic = staticmethod(generate_from_synthetic)
    generateFromSynthetic = staticmethod(generate_from_synthetic)
    dependency_graph = staticmethod(dependency_graph)
    dependencyGraph = staticmethod(dependency_graph)
    normalize = staticmethod(normalize)
    lookup = staticmethod(lookup)
    load_knowledge_base = staticmethod(load_knowledge_base)
    loadKnowledgeBase = staticmethod(load_knowledge_base)
    read_knowledge_base = staticmethod(read_knowledge_base)
    readKnowledgeBase = staticmethod(read_knowledge_base)
    clear_knowledge_base_for_testing = staticmethod(clear_knowledge_base_for_testing)
    clearKnowledgeBaseForTesting = staticmethod(clear_knowledge_base_for_testing)
