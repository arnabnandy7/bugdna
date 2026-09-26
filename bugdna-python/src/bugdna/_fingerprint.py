from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Iterable, Union

from ._category import FailureCategory
from ._family import FailureFamily
from ._priority import FailurePriority
from ._shape import simple_class_name


@dataclass(frozen=True, eq=False)
class Fingerprint:
    """An immutable identity for a unique failure."""

    id: str
    root_cause: str
    signature: str
    qualified_signature: str
    frames: tuple[str, ...]
    failure_chain: tuple[str, ...]
    cause_chain: tuple[str, ...]
    explanation: str
    stability_score: int
    priority: FailurePriority
    category: FailureCategory
    family: FailureFamily

    def __init__(
        self,
        id: str,
        root_cause: str,
        signature: str,
        qualified_signature: str,
        frames: Iterable[str],
        failure_chain: Iterable[str],
        cause_chain: Iterable[str],
        explanation: str,
        stability_score: int,
        priority: Union[FailurePriority, str],
        category: Union[FailureCategory, str],
        family: Union[FailureFamily, str],
    ) -> None:
        if not id:
            raise ValueError("id must not be empty")
        if not root_cause:
            raise ValueError("root_cause must not be empty")
        if signature is None:
            raise ValueError("signature must not be None")
        if qualified_signature is None:
            raise ValueError("qualified_signature must not be None")
        if frames is None:
            raise ValueError("frames must not be None")
        if failure_chain is None:
            raise ValueError("failure_chain must not be None")
        if cause_chain is None:
            raise ValueError("cause_chain must not be None")
        if explanation is None:
            raise ValueError("explanation must not be None")
        if stability_score < 0 or stability_score > 100:
            raise ValueError("stability_score must be between 0 and 100")

        object.__setattr__(self, "id", id)
        object.__setattr__(self, "root_cause", root_cause)
        object.__setattr__(self, "signature", signature)
        object.__setattr__(self, "qualified_signature", qualified_signature)
        object.__setattr__(self, "frames", tuple(frames))
        object.__setattr__(self, "failure_chain", tuple(failure_chain))
        object.__setattr__(self, "cause_chain", tuple(cause_chain))
        object.__setattr__(self, "explanation", explanation)
        object.__setattr__(self, "stability_score", int(stability_score))
        object.__setattr__(
            self,
            "priority",
            priority
            if isinstance(priority, FailurePriority)
            else FailurePriority(priority),
        )
        object.__setattr__(
            self,
            "category",
            category
            if isinstance(category, FailureCategory)
            else FailureCategory(category),
        )
        object.__setattr__(
            self,
            "family",
            family if isinstance(family, FailureFamily) else FailureFamily(family),
        )

    @property
    def rootCause(self) -> str:
        return self.root_cause

    @property
    def qualifiedSignature(self) -> str:
        return self.qualified_signature

    @property
    def failureChain(self) -> tuple[str, ...]:
        return self.failure_chain

    @property
    def causeChain(self) -> tuple[str, ...]:
        return self.cause_chain

    @property
    def stabilityScore(self) -> int:
        return self.stability_score

    def get_id(self) -> str:
        return self.id

    def getId(self) -> str:
        return self.id

    def get_root_cause(self) -> str:
        return self.root_cause

    def getRootCause(self) -> str:
        return self.root_cause

    def get_signature(self) -> str:
        return self.signature

    def getSignature(self) -> str:
        return self.signature

    def get_qualified_signature(self) -> str:
        return self.qualified_signature

    def getQualifiedSignature(self) -> str:
        return self.qualified_signature

    def get_frames(self) -> tuple[str, ...]:
        return self.frames

    def getFrames(self) -> tuple[str, ...]:
        return self.frames

    def get_failure_chain(self) -> tuple[str, ...]:
        return self.failure_chain

    def getFailureChain(self) -> tuple[str, ...]:
        return self.failure_chain

    def get_cause_chain(self) -> tuple[str, ...]:
        return self.cause_chain

    def getCauseChain(self) -> tuple[str, ...]:
        return self.cause_chain

    def get_explanation(self) -> str:
        return self.explanation

    def getExplanation(self) -> str:
        return self.explanation

    def get_stability_score(self) -> int:
        return self.stability_score

    def getStabilityScore(self) -> int:
        return self.stability_score

    def get_priority(self) -> FailurePriority:
        return self.priority

    def getPriority(self) -> FailurePriority:
        return self.priority

    def get_category(self) -> FailureCategory:
        return self.category

    def getCategory(self) -> FailureCategory:
        return self.category

    def get_family(self) -> FailureFamily:
        return self.family

    def getFamily(self) -> FailureFamily:
        return self.family

    def explain(self) -> str:
        return (
            f"{self.id}\n\n"
            f"Root Cause:\n{simple_class_name(self.root_cause)}\n\n"
            f"Origin:\n{self.signature}\n\n"
            f"Confidence:\n{self.stability_score}%\n\n"
            f"Failure Chain:\n{' -> '.join(self.failure_chain)}"
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "rootCause": self.root_cause,
            "signature": self.signature,
            "qualifiedSignature": self.qualified_signature,
            "frames": list(self.frames),
            "failureChain": list(self.failure_chain),
            "causeChain": list(self.cause_chain),
            "stabilityScore": self.stability_score,
            "priority": self.priority.value,
            "category": self.category.value,
            "family": self.family.value,
        }

    def __eq__(self, other: object) -> bool:
        if self is other:
            return True
        if not isinstance(other, Fingerprint):
            return False
        return self.id == other.id

    def __hash__(self) -> int:
        return hash(self.id)

    def __str__(self) -> str:
        return (
            f"Fingerprint{{id='{self.id}', rootCause='{self.root_cause}', "
            f"signature='{self.signature}', stabilityScore={self.stability_score}, "
            f"priority={self.priority.value}, category={self.category.value}, "
            f"family={self.family.value}}}"
        )
