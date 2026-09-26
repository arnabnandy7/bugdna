from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Iterable, Mapping, Union

from ._fingerprint import Fingerprint
from ._shape import (
    equal_score,
    frame_similarity,
    java_round,
    method_similarity,
    overlap,
    parse_signature,
)

_SAME_ID_SCORE = 100
_ROOT_CAUSE_WEIGHT = 35
_ORIGIN_CLASS_WEIGHT = 25
_METHOD_WEIGHT = 20
_FRAME_WEIGHT = 15
_CAUSE_CHAIN_WEIGHT = 5
_LIKELY_RELATED_THRESHOLD = 80


@dataclass(frozen=True)
class Similarity:
    percentage: int
    explanation: str

    def __post_init__(self) -> None:
        if self.percentage < 0 or self.percentage > 100:
            raise ValueError("percentage must be between 0 and 100")
        if not self.explanation:
            raise ValueError("explanation must not be empty")

    @property
    def is_likely_related(self) -> bool:
        return self.percentage >= _LIKELY_RELATED_THRESHOLD

    @property
    def isLikelyRelated(self) -> bool:
        return self.is_likely_related

    def get_percentage(self) -> int:
        return self.percentage

    def getPercentage(self) -> int:
        return self.percentage

    def get_explanation(self) -> str:
        return self.explanation

    def getExplanation(self) -> str:
        return self.explanation


def _extract_shape(
    item: Union[Fingerprint, Mapping[str, Any]],
) -> tuple[str, str, str, Iterable[str], Iterable[str]]:
    if isinstance(item, Fingerprint):
        return (
            item.id,
            item.root_cause,
            item.qualified_signature,
            item.frames,
            item.cause_chain,
        )
    if isinstance(item, Mapping):
        return (
            str(item["id"]),
            str(item.get("root_cause", item.get("rootCause", ""))),
            str(item.get("qualified_signature", item.get("qualifiedSignature", ""))),
            item.get("frames", ()),
            item.get("cause_chain", item.get("causeChain", ())),
        )
    raise TypeError("Expected Fingerprint or Mapping")


def compare_fingerprints(
    first: Union[Fingerprint, Mapping[str, Any]],
    second: Union[Fingerprint, Mapping[str, Any]],
) -> Similarity:
    if first is None:
        raise ValueError("first must not be None")
    if second is None:
        raise ValueError("second must not be None")

    first_id, first_rc, first_qsig, first_frames, first_causes = _extract_shape(first)
    second_id, second_rc, second_qsig, second_frames, second_causes = _extract_shape(
        second
    )

    if first_id == second_id:
        return Similarity(
            percentage=_SAME_ID_SCORE,
            explanation="Fingerprints have the same id and represent the same failure group.",
        )

    first_sig = parse_signature(first_qsig)
    second_sig = parse_signature(second_qsig)

    root_score = equal_score(first_rc, second_rc, _ROOT_CAUSE_WEIGHT)
    class_score = equal_score(
        first_sig.class_name, second_sig.class_name, _ORIGIN_CLASS_WEIGHT
    )
    method_score = java_round(
        _METHOD_WEIGHT
        * method_similarity(first_sig.method_name, second_sig.method_name)
    )
    frame_score = java_round(
        _FRAME_WEIGHT * frame_similarity(first_frames, second_frames)
    )
    cause_score = java_round(
        _CAUSE_CHAIN_WEIGHT * overlap(first_causes, second_causes)
    )
    total = root_score + class_score + method_score + frame_score + cause_score

    explanation = (
        f"Similarity {total}% between {first_id} and {second_id} from "
        f"rootCause={root_score}, originClass={class_score}, method={method_score}, "
        f"frames={frame_score}, causeChain={cause_score}."
    )
    return Similarity(percentage=total, explanation=explanation)


class BugSimilarity:
    @staticmethod
    def compare(
        first: Union[Fingerprint, Mapping[str, Any]],
        second: Union[Fingerprint, Mapping[str, Any]],
    ) -> Similarity:
        return compare_fingerprints(first, second)
