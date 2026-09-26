from __future__ import annotations

from enum import Enum
from typing import Any, Mapping, Union

from ._context import FailureContext


class FailurePriority(str, Enum):
    UNKNOWN = "UNKNOWN"
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


def prioritize(
    context: Union[FailureContext, Mapping[str, Any], None] = None,
) -> FailurePriority:
    ctx = FailureContext.from_input(context)
    if not ctx.has_impact_data():
        return FailurePriority.UNKNOWN
    if ctx.fatal or ctx.affected_users >= 100 or ctx.occurrences >= 1000:
        return FailurePriority.CRITICAL
    if ctx.affected_users >= 10 or ctx.occurrences >= 100:
        return FailurePriority.HIGH
    if ctx.affected_users > 0 or ctx.occurrences >= 10:
        return FailurePriority.MEDIUM
    return FailurePriority.LOW
