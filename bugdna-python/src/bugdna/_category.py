from __future__ import annotations

from enum import Enum


class FailureCategory(str, Enum):
    DATABASE = "DATABASE"
    NETWORK = "NETWORK"
    VALIDATION = "VALIDATION"
    SECURITY = "SECURITY"
    SERIALIZATION = "SERIALIZATION"
    CONFIGURATION = "CONFIGURATION"
    BUSINESS = "BUSINESS"
    UNKNOWN = "UNKNOWN"


_DATABASE_PATTERNS = (
    "java.sql.",
    ".sql",
    "database",
    "jdbc",
    "datasource",
    "sqlite3",
    "psycopg",
    "sqlalchemy",
    "pymysql",
    "asyncpg",
    "pymongo",
    "operationalerror",
    "integrityerror",
)

_NETWORK_PATTERNS = (
    "java.net.",
    "socket",
    "connect",
    "network",
    "timeout",
    "http",
    "connectionerror",
    "connectionrefusederror",
    "connectionreseterror",
    "brokenpipeerror",
    "timeouterror",
    "urlerror",
)

_VALIDATION_PATTERNS = (
    "validation",
    "constraint",
    "illegalargument",
    "parse",
    "format",
    "valueerror",
    "assertionerror",
)

_SECURITY_PATTERNS = (
    "security",
    "accessdenied",
    "authentication",
    "authorization",
    "permission",
    "certificate",
    "ssl",
    "crypto",
    "permissionerror",
)

_SERIALIZATION_PATTERNS = (
    "serialization",
    "deserialization",
    "invalidclass",
    "invalidobject",
    "notserializable",
    "objectstream",
    "streamcorrupted",
    "json",
    "xml",
    "mapping",
    "codec",
    "decode",
    "encode",
    "unicodedecodeerror",
    "unicodeencodeerror",
    "pickleerror",
)

_CONFIGURATION_PATTERNS = (
    "configuration",
    "config",
    "property",
    "environment",
    "missingresource",
)

_BUSINESS_PATTERNS = (
    "business",
    "domain",
    "rule",
    "policy",
)


def _matches_any(value: str, patterns: tuple[str, ...]) -> bool:
    return any(pattern in value for pattern in patterns)


def categorize(root_cause_name: str) -> FailureCategory:
    if root_cause_name is None:
        raise ValueError("root_cause_name must not be None")
    lower_name = root_cause_name.lower()

    if _matches_any(lower_name, _DATABASE_PATTERNS):
        return FailureCategory.DATABASE
    if _matches_any(lower_name, _NETWORK_PATTERNS):
        return FailureCategory.NETWORK
    if _matches_any(lower_name, _VALIDATION_PATTERNS):
        return FailureCategory.VALIDATION
    if _matches_any(lower_name, _SECURITY_PATTERNS):
        return FailureCategory.SECURITY
    if _matches_any(lower_name, _SERIALIZATION_PATTERNS):
        return FailureCategory.SERIALIZATION
    if _matches_any(lower_name, _CONFIGURATION_PATTERNS):
        return FailureCategory.CONFIGURATION
    if _matches_any(lower_name, _BUSINESS_PATTERNS):
        return FailureCategory.BUSINESS

    return FailureCategory.UNKNOWN
