from __future__ import annotations

from enum import Enum
from typing import Iterable, Union

from ._category import FailureCategory
from ._normalize import normalize


class FailureFamily(str, Enum):
    DATABASE_CONNECTIVITY = "DATABASE_CONNECTIVITY"
    DATABASE_OPERATION = "DATABASE_OPERATION"
    NETWORK_CONNECTIVITY = "NETWORK_CONNECTIVITY"
    VALIDATION = "VALIDATION"
    SECURITY = "SECURITY"
    SERIALIZATION = "SERIALIZATION"
    CONFIGURATION = "CONFIGURATION"
    BUSINESS = "BUSINESS"
    UNKNOWN = "UNKNOWN"


_DATABASE_CONTEXT_PATTERNS = (
    "java.sql.",
    ".sql",
    "database",
    "jdbc",
    "datasource",
    "hikari",
    "connectionpool",
    "poolbase",
    "postgres",
    "mysql",
    "mariadb",
    "oracle",
    "sqlite3",
    "psycopg",
    "sqlalchemy",
    "pymysql",
    "asyncpg",
)

_CONNECTIVITY_PATTERNS = (
    "connectexception",
    "socketexception",
    "sockettimeoutexception",
    "sqltransientconnectionexception",
    "sqlnontransientconnectionexception",
    "sqlrecoverableexception",
    "sqltimeoutexception",
    "jdbcconnectionexception",
    "connection refused",
    "connection reset",
    "connection timed out",
    "socket timeout",
    "communications link failure",
    "unable to acquire connection",
    "could not open connection",
    "pool exhausted",
    "connection pool",
    "timeout",
    "connectionerror",
    "connectionrefusederror",
    "connectionreseterror",
    "timeouterror",
)


def _matches_any(value: str, patterns: tuple[str, ...]) -> bool:
    return any(pattern in value for pattern in patterns)


def classify_family_from_evidence(
    category: Union[FailureCategory, str],
    evidence: str,
) -> FailureFamily:
    cat = (
        category
        if isinstance(category, FailureCategory)
        else FailureCategory(category)
    )
    lower_evidence = evidence.lower()
    connectivity = _matches_any(lower_evidence, _CONNECTIVITY_PATTERNS)
    database_context = cat == FailureCategory.DATABASE or _matches_any(
        lower_evidence, _DATABASE_CONTEXT_PATTERNS
    )

    if connectivity and database_context:
        return FailureFamily.DATABASE_CONNECTIVITY
    if cat == FailureCategory.DATABASE:
        return FailureFamily.DATABASE_OPERATION
    if cat == FailureCategory.NETWORK:
        return FailureFamily.NETWORK_CONNECTIVITY
    if cat == FailureCategory.VALIDATION:
        return FailureFamily.VALIDATION
    if cat == FailureCategory.SECURITY:
        return FailureFamily.SECURITY
    if cat == FailureCategory.SERIALIZATION:
        return FailureFamily.SERIALIZATION
    if cat == FailureCategory.CONFIGURATION:
        return FailureFamily.CONFIGURATION
    if cat == FailureCategory.BUSINESS:
        return FailureFamily.BUSINESS
    return FailureFamily.UNKNOWN


def build_family_evidence(
    root_cause_name: str,
    cause_chain_names: Iterable[str],
    messages: Iterable[str],
    frames: Iterable[str],
) -> str:
    parts: list[str] = [root_cause_name]
    parts.extend(cause_chain_names)
    for msg in messages:
        if msg:
            parts.append(normalize(msg))
    parts.extend(frames)
    return " ".join(parts).lower()
