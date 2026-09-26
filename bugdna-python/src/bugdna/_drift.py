from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Iterable, Mapping, Union

from ._fingerprint import Fingerprint
from ._shape import frame_similarity, java_round, method_similarity, parse_signature

_ORIGIN_CLASS_WEIGHT = 27
_METHOD_WEIGHT = 27
_FRAME_WEIGHT = 46
_CODE_PATH_CHANGE_THRESHOLD = 50


@dataclass(frozen=True)
class FingerprintDrift:
    id: str
    old_fingerprint: Any
    new_fingerprint: Any
    signature_drift_percentage: int

    def __post_init__(self) -> None:
        if self.signature_drift_percentage < 0 or self.signature_drift_percentage > 100:
            raise ValueError("signature_drift_percentage must be between 0 and 100")

    @property
    def signatureDriftPercentage(self) -> int:
        return self.signature_drift_percentage

    @property
    def is_possible_code_path_change(self) -> bool:
        return self.signature_drift_percentage >= _CODE_PATH_CHANGE_THRESHOLD

    @property
    def isPossibleCodePathChange(self) -> bool:
        return self.is_possible_code_path_change

    def get_id(self) -> str:
        return self.id

    def getId(self) -> str:
        return self.id

    def get_old_fingerprint(self) -> Any:
        return self.old_fingerprint

    def getOldFingerprint(self) -> Any:
        return self.old_fingerprint

    def get_new_fingerprint(self) -> Any:
        return self.new_fingerprint

    def getNewFingerprint(self) -> Any:
        return self.new_fingerprint

    def get_signature_drift_percentage(self) -> int:
        return self.signature_drift_percentage

    def getSignatureDriftPercentage(self) -> int:
        return self.signature_drift_percentage

    def report(self) -> str:
        message = (
            "Possible code path change detected"
            if self.is_possible_code_path_change
            else "Minor signature shape change detected"
        )
        return (
            f"{self.id}\n"
            f"Signature Drift: {self.signature_drift_percentage}%\n"
            f"{message}"
        )


def _extract_drift_fields(
    item: Union[Fingerprint, Mapping[str, Any]],
) -> tuple[str, str, Iterable[str]]:
    if isinstance(item, Fingerprint):
        return item.id, item.qualified_signature, item.frames
    if isinstance(item, Mapping):
        return (
            str(item["id"]),
            str(item.get("qualified_signature", item.get("qualifiedSignature", ""))),
            item.get("frames", ()),
        )
    raise TypeError("Expected Fingerprint or Mapping")


def detect_drift(
    old_fingerprint: Union[Fingerprint, Mapping[str, Any]],
    new_fingerprint: Union[Fingerprint, Mapping[str, Any]],
) -> FingerprintDrift:
    if old_fingerprint is None:
        raise ValueError("old_fingerprint must not be None")
    if new_fingerprint is None:
        raise ValueError("new_fingerprint must not be None")

    old_id, old_qsig, old_frames = _extract_drift_fields(old_fingerprint)
    new_id, new_qsig, new_frames = _extract_drift_fields(new_fingerprint)

    if old_id != new_id:
        raise ValueError("fingerprint IDs must match")

    old_sig = parse_signature(old_qsig)
    new_sig = parse_signature(new_qsig)

    class_score = (
        _ORIGIN_CLASS_WEIGHT if old_sig.class_name == new_sig.class_name else 0
    )
    method_score = java_round(
        _METHOD_WEIGHT
        * method_similarity(old_sig.method_name, new_sig.method_name)
    )
    frame_score = java_round(
        _FRAME_WEIGHT * frame_similarity(old_frames, new_frames)
    )
    similarity = class_score + method_score + frame_score

    return FingerprintDrift(
        id=old_id,
        old_fingerprint=old_fingerprint,
        new_fingerprint=new_fingerprint,
        signature_drift_percentage=100 - similarity,
    )


def has_signature_drift(
    old_fingerprint: Union[Fingerprint, Mapping[str, Any]],
    new_fingerprint: Union[Fingerprint, Mapping[str, Any]],
) -> bool:
    return detect_drift(old_fingerprint, new_fingerprint).signature_drift_percentage > 0


class FingerprintDriftDetector:
    @staticmethod
    def detect(
        old_fingerprint: Union[Fingerprint, Mapping[str, Any]],
        new_fingerprint: Union[Fingerprint, Mapping[str, Any]],
    ) -> FingerprintDrift:
        return detect_drift(old_fingerprint, new_fingerprint)

    @staticmethod
    def has_signature_drift(
        old_fingerprint: Union[Fingerprint, Mapping[str, Any]],
        new_fingerprint: Union[Fingerprint, Mapping[str, Any]],
    ) -> bool:
        return has_signature_drift(old_fingerprint, new_fingerprint)
