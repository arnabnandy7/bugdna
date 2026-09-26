from __future__ import annotations

import re

_EMAIL_PATTERN = re.compile(
    r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b",
    re.IGNORECASE,
)
_NUMBER_PATTERN = re.compile(r"\b\d+\b")


def normalize(value: str) -> str:
    """Replaces emails and numeric tokens with stable placeholders."""
    if value is None:
        raise ValueError("value must not be None")
    masked_emails = _EMAIL_PATTERN.sub("{EMAIL}", value)
    return _NUMBER_PATTERN.sub("{NUMBER}", masked_emails)
