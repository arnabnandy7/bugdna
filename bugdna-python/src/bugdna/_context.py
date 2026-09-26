from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping, Union


@dataclass(frozen=True)
class FailureContext:
    """Impact metadata used to compute dynamic failure priority."""

    occurrences: int = -1
    affected_users: int = -1
    fatal: bool = False

    @property
    def affectedUsers(self) -> int:
        return self.affected_users

    def has_impact_data(self) -> bool:
        return self.occurrences >= 0 and self.affected_users >= 0

    def hasImpactData(self) -> bool:
        return self.has_impact_data()

    def getOccurrences(self) -> int:
        return self.occurrences

    def getAffectedUsers(self) -> int:
        return self.affected_users

    def isFatal(self) -> bool:
        return self.fatal

    @staticmethod
    def unknown() -> FailureContext:
        return _UNKNOWN_CONTEXT

    @staticmethod
    def of(occurrences: int, affected_users: int, fatal: bool = False) -> FailureContext:
        if occurrences < 0:
            raise ValueError("occurrences must not be negative")
        if affected_users < 0:
            raise ValueError("affected_users must not be negative")
        return FailureContext(
            occurrences=int(occurrences),
            affected_users=int(affected_users),
            fatal=bool(fatal),
        )

    @staticmethod
    def from_input(
        value: Union[FailureContext, Mapping[str, Any], None] = None,
    ) -> FailureContext:
        if value is None:
            return _UNKNOWN_CONTEXT
        if isinstance(value, FailureContext):
            return value
        if isinstance(value, Mapping):
            occ = value.get("occurrences", -1)
            users = value.get("affected_users", value.get("affectedUsers", -1))
            fatal = bool(value.get("fatal", False))
            return FailureContext(
                occurrences=int(occ),
                affected_users=int(users),
                fatal=fatal,
            )
        raise TypeError("Invalid FailureContext input")


_UNKNOWN_CONTEXT = FailureContext(-1, -1, False)
