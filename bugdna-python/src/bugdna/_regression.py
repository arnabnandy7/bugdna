from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable

from ._drift import FingerprintDrift, detect_drift, has_signature_drift
from ._fingerprint import Fingerprint


@dataclass(frozen=True)
class DeploymentSnapshot:
    version: str
    fingerprints: tuple[Fingerprint, ...]

    def __init__(self, version: str, fingerprints: Iterable[Fingerprint]) -> None:
        if not version or not version.strip():
            raise ValueError("version must not be blank")
        if fingerprints is None:
            raise ValueError("fingerprints must not be None")
        by_id: dict[str, Fingerprint] = {}
        for fp in fingerprints:
            if fp is None:
                raise ValueError("fingerprint must not be None")
            if fp.id not in by_id:
                by_id[fp.id] = fp
        sorted_fps = tuple(sorted(by_id.values(), key=lambda x: x.id))
        object.__setattr__(self, "version", version)
        object.__setattr__(self, "fingerprints", sorted_fps)

    def get_version(self) -> str:
        return self.version

    def getVersion(self) -> str:
        return self.version

    def get_fingerprints(self) -> tuple[Fingerprint, ...]:
        return self.fingerprints

    def getFingerprints(self) -> tuple[Fingerprint, ...]:
        return self.fingerprints


@dataclass(frozen=True)
class DeploymentComparison:
    old_version: str
    new_version: str
    new_fingerprints: tuple[Fingerprint, ...]
    resolved_fingerprints: tuple[Fingerprint, ...]
    recurring_fingerprints: tuple[Fingerprint, ...]
    fingerprint_drifts: tuple[FingerprintDrift, ...] = ()

    def __init__(
        self,
        old_version: str,
        new_version: str,
        new_fingerprints: Iterable[Fingerprint],
        resolved_fingerprints: Iterable[Fingerprint],
        recurring_fingerprints: Iterable[Fingerprint],
        fingerprint_drifts: Iterable[FingerprintDrift] = (),
    ) -> None:
        object.__setattr__(self, "old_version", old_version)
        object.__setattr__(self, "new_version", new_version)
        object.__setattr__(self, "new_fingerprints", tuple(new_fingerprints))
        object.__setattr__(self, "resolved_fingerprints", tuple(resolved_fingerprints))
        object.__setattr__(
            self, "recurring_fingerprints", tuple(recurring_fingerprints)
        )
        object.__setattr__(self, "fingerprint_drifts", tuple(fingerprint_drifts))

    @property
    def new_fingerprint_count(self) -> int:
        return len(self.new_fingerprints)

    @property
    def resolved_fingerprint_count(self) -> int:
        return len(self.resolved_fingerprints)

    @property
    def recurring_fingerprint_count(self) -> int:
        return len(self.recurring_fingerprints)

    @property
    def fingerprint_drift_count(self) -> int:
        return len(self.fingerprint_drifts)

    def getOldVersion(self) -> str:
        return self.old_version

    def getNewVersion(self) -> str:
        return self.new_version

    def getNewFingerprints(self) -> tuple[Fingerprint, ...]:
        return self.new_fingerprints

    def getResolvedFingerprints(self) -> tuple[Fingerprint, ...]:
        return self.resolved_fingerprints

    def getRecurringFingerprints(self) -> tuple[Fingerprint, ...]:
        return self.recurring_fingerprints

    def getFingerprintDrifts(self) -> tuple[FingerprintDrift, ...]:
        return self.fingerprint_drifts

    def getNewFingerprintCount(self) -> int:
        return self.new_fingerprint_count

    def getResolvedFingerprintCount(self) -> int:
        return self.resolved_fingerprint_count

    def getRecurringFingerprintCount(self) -> int:
        return self.recurring_fingerprint_count

    def getFingerprintDriftCount(self) -> int:
        return self.fingerprint_drift_count

    def report(self) -> str:
        out = (
            f"Version {self.old_version} -> Version {self.new_version}\n\n"
            f"New fingerprints: {self.new_fingerprint_count}\n"
            f"Resolved fingerprints: {self.resolved_fingerprint_count}\n"
            f"Recurring fingerprints: {self.recurring_fingerprint_count}"
        )
        if self.fingerprint_drifts:
            out += f"\nFingerprint drifts: {self.fingerprint_drift_count}"
            for drift in self.fingerprint_drifts:
                out += f"\n\n{drift.report()}"
        return out


class RegressionDetector:
    @staticmethod
    def compare(
        old_deployment: DeploymentSnapshot,
        new_deployment: DeploymentSnapshot,
    ) -> DeploymentComparison:
        if old_deployment is None:
            raise ValueError("old_deployment must not be None")
        if new_deployment is None:
            raise ValueError("new_deployment must not be None")

        old_map = {fp.id: fp for fp in old_deployment.fingerprints}
        new_map = {fp.id: fp for fp in new_deployment.fingerprints}

        added: list[Fingerprint] = []
        recurring: list[Fingerprint] = []
        drifts: list[FingerprintDrift] = []

        for fid, new_fp in new_map.items():
            old_fp = old_map.get(fid)
            if old_fp is not None:
                recurring.append(new_fp)
                if has_signature_drift(old_fp, new_fp):
                    drifts.append(detect_drift(old_fp, new_fp))
            else:
                added.append(new_fp)

        resolved = [
            old_fp for fid, old_fp in old_map.items() if fid not in new_map
        ]

        return DeploymentComparison(
            old_version=old_deployment.version,
            new_version=new_deployment.version,
            new_fingerprints=added,
            resolved_fingerprints=resolved,
            recurring_fingerprints=recurring,
            fingerprint_drifts=drifts,
        )
