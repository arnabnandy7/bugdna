from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping, Union

from ._bugdna import generate
from ._fingerprint import Fingerprint
from ._shape import parse_signature, simple_class_name


@dataclass(frozen=True)
class FingerprintDiff:
    summary: str
    old_value: str
    new_value: str
    explanation: str

    @property
    def oldValue(self) -> str:
        return self.old_value

    @property
    def newValue(self) -> str:
        return self.new_value

    def get_summary(self) -> str:
        return self.summary

    def getSummary(self) -> str:
        return self.summary

    def get_old_value(self) -> str:
        return self.old_value

    def getOldValue(self) -> str:
        return self.old_value

    def get_new_value(self) -> str:
        return self.new_value

    def getNewValue(self) -> str:
        return self.new_value

    def get_explanation(self) -> str:
        return self.explanation

    def getExplanation(self) -> str:
        return self.explanation

    def explain(self) -> str:
        return f"{self.summary}\n\nOld:\n{self.old_value}\n\nNew:\n{self.new_value}"

    def __str__(self) -> str:
        return (
            f"FingerprintDiff{{summary='{self.summary}', oldValue='{self.old_value}', "
            f"newValue='{self.new_value}', explanation='{self.explanation}'}}"
        )


def detect_layer(class_name: str) -> str:
    if class_name.endswith("Controller"):
        return "Controller"
    if class_name.endswith("Service"):
        return "Service"
    if class_name.endswith("Repository"):
        return "Repository"
    if class_name.endswith("Gateway"):
        return "Gateway"
    if class_name.endswith("Client"):
        return "Client"
    if class_name.endswith("Handler"):
        return "Handler"
    if class_name.endswith("Validator"):
        return "Validator"
    if class_name.endswith("Configuration") or class_name.endswith("Config"):
        return "Configuration"
    if class_name.endswith("Mapper"):
        return "Mapper"
    if class_name.endswith("Codec"):
        return "Codec"
    return "Application"


def _extract_diff_fields(
    item: Union[Fingerprint, Mapping[str, Any]],
) -> tuple[str, str, str, str]:
    if isinstance(item, Fingerprint):
        return (
            item.id,
            item.root_cause,
            item.qualified_signature,
            item.signature,
        )
    if isinstance(item, Mapping):
        fid = str(item["id"])
        rc = str(item.get("root_cause", item.get("rootCause", "")))
        qsig = str(
            item.get("qualified_signature", item.get("qualifiedSignature", ""))
        )
        sig = item.get("signature")
        if not sig:
            origin = parse_signature(qsig)
            simple = simple_class_name(origin.class_name)
            sig = f"{simple}#{origin.method_name}" if origin.method_name else simple
        return fid, rc, qsig, str(sig)
    raise TypeError("Expected Fingerprint or Mapping")


def diff_fingerprints(
    old_fingerprint: Union[Fingerprint, Mapping[str, Any]],
    new_fingerprint: Union[Fingerprint, Mapping[str, Any]],
) -> FingerprintDiff:
    if old_fingerprint is None:
        raise ValueError("old_fingerprint must not be None")
    if new_fingerprint is None:
        raise ValueError("new_fingerprint must not be None")

    old_id, old_rc, old_qsig, old_sig = _extract_diff_fields(old_fingerprint)
    new_id, new_rc, new_qsig, new_sig = _extract_diff_fields(new_fingerprint)

    if old_id == new_id:
        return FingerprintDiff(
            summary="No Fingerprint Change",
            old_value=old_id,
            new_value=new_id,
            explanation="Both failures resolve to the same fingerprint.",
        )

    old_origin = parse_signature(old_qsig)
    new_origin = parse_signature(new_qsig)
    old_class = simple_class_name(old_origin.class_name)
    new_class = simple_class_name(new_origin.class_name)
    old_layer = detect_layer(old_class)
    new_layer = detect_layer(new_class)

    if old_class != new_class and old_layer == new_layer:
        return FingerprintDiff(
            summary=f"{old_layer} Layer Changed",
            old_value=old_class,
            new_value=new_class,
            explanation=f"Origin moved within the {old_layer} layer.",
        )

    if old_layer != new_layer:
        return FingerprintDiff(
            summary="Layer Changed",
            old_value=old_layer,
            new_value=new_layer,
            explanation=f"Origin moved from the {old_layer} layer to the {new_layer} layer.",
        )

    if old_origin.method_name != new_origin.method_name:
        return FingerprintDiff(
            summary="Method Changed",
            old_value=old_origin.method_name,
            new_value=new_origin.method_name,
            explanation=f"Origin method changed in {old_class}.",
        )

    if old_rc != new_rc:
        return FingerprintDiff(
            summary="Root Cause Changed",
            old_value=simple_class_name(old_rc),
            new_value=simple_class_name(new_rc),
            explanation="Root-cause exception type changed.",
        )

    return FingerprintDiff(
        summary="Call Path Changed",
        old_value=old_sig,
        new_value=new_sig,
        explanation="Fingerprint changed because the normalized call path changed.",
    )


def diff_exceptions(
    old_exception: BaseException,
    new_exception: BaseException,
) -> FingerprintDiff:
    if old_exception is None:
        raise ValueError("old_exception must not be None")
    if new_exception is None:
        raise ValueError("new_exception must not be None")
    return diff_fingerprints(generate(old_exception), generate(new_exception))


class BugDiff:
    @staticmethod
    def compare(
        old_item: Union[BaseException, Fingerprint, Mapping[str, Any]],
        new_item: Union[BaseException, Fingerprint, Mapping[str, Any]],
    ) -> FingerprintDiff:
        if isinstance(old_item, BaseException) and isinstance(new_item, BaseException):
            return diff_exceptions(old_item, new_item)
        return diff_fingerprints(old_item, new_item)  # type: ignore[arg-type]
