from __future__ import annotations

from dataclasses import dataclass
import math
import re
from typing import Iterable


@dataclass(frozen=True)
class SignatureParts:
    class_name: str
    method_name: str

    @property
    def className(self) -> str:
        return self.class_name

    @property
    def methodName(self) -> str:
        return self.method_name


def java_round(value: float) -> int:
    """Matches Java Math.round(double) behavior: floor(x + 0.5)."""
    return int(math.floor(value + 0.5))


def parse_signature(qualified_signature: str) -> SignatureParts:
    separator = qualified_signature.rfind("#")
    if separator < 0:
        return SignatureParts(class_name=qualified_signature, method_name="")
    return SignatureParts(
        class_name=qualified_signature[:separator],
        method_name=qualified_signature[separator + 1 :],
    )


def simple_class_name(class_name: str) -> str:
    package_separator = class_name.rfind(".")
    simple_name = (
        class_name[package_separator + 1 :]
        if package_separator >= 0
        else class_name
    )
    return simple_name.replace("$", ".")


def equal_score(first: str, second: str, weight: int) -> int:
    return weight if first == second else 0


_CAMEL_CASE_RE = re.compile(r"([a-z])([A-Z])")
_WHITESPACE_RE = re.compile(r"\s+")


def _method_tokens(method_name: str) -> set[str]:
    normalized = (
        _CAMEL_CASE_RE.sub(r"\1 \2", method_name)
        .replace("_", " ")
        .replace("-", " ")
        .lower()
    )
    return {part for part in _WHITESPACE_RE.split(normalized) if part}


def _set_overlap(first_values: set[str], second_values: set[str]) -> float:
    intersection = first_values & second_values
    union = first_values | second_values
    if not union:
        return 0.0
    return len(intersection) / len(union)


def method_similarity(first: str, second: str) -> float:
    if first == second:
        return 1.0
    if not first or not second:
        return 0.0
    if first.startswith(second) or second.startswith(first):
        return 0.67

    first_tokens = _method_tokens(first)
    second_tokens = _method_tokens(second)
    if not first_tokens or not second_tokens:
        return 0.0

    return _set_overlap(first_tokens, second_tokens)


def _single_frame_similarity(first: str, second: str) -> float:
    if first == second:
        return 1.0
    first_frame = parse_signature(first)
    second_frame = parse_signature(second)
    if first_frame.class_name != second_frame.class_name:
        return 0.0
    return 0.7 + 0.3 * method_similarity(
        first_frame.method_name, second_frame.method_name
    )


def frame_similarity(first: Iterable[str], second: Iterable[str]) -> float:
    first_frames = set(first)
    second_frames = set(second)
    if not first_frames or not second_frames:
        return 0.0

    total = 0.0
    for first_frame in first_frames:
        best = max(
            (_single_frame_similarity(first_frame, second_frame) for second_frame in second_frames),
            default=0.0,
        )
        total += best

    return total / max(len(first_frames), len(second_frames))


def overlap(first: Iterable[str], second: Iterable[str]) -> float:
    first_values = set(first)
    second_values = set(second)
    if not first_values or not second_values:
        return 0.0
    return _set_overlap(first_values, second_values)
