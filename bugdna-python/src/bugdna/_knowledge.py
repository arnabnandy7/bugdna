from __future__ import annotations

from dataclasses import dataclass
import os
from pathlib import Path
from types import MappingProxyType
from typing import Mapping, Optional, Union


@dataclass(frozen=True)
class FingerprintKnowledge:
    id: str
    fields: Mapping[str, str]

    def __init__(self, id: str, fields: Mapping[str, str]) -> None:
        if not id or not id.strip():
            raise ValueError("id must not be blank")
        if fields is None:
            raise ValueError("fields must not be None")
        object.__setattr__(self, "id", id)
        object.__setattr__(self, "fields", MappingProxyType(dict(fields)))

    @property
    def title(self) -> Optional[str]:
        return self.fields.get("title")

    @property
    def owner(self) -> Optional[str]:
        return self.fields.get("owner")

    @property
    def runbook(self) -> Optional[str]:
        return self.fields.get("runbook")

    def get_id(self) -> str:
        return self.id

    def getId(self) -> str:
        return self.id

    def get_title(self) -> Optional[str]:
        return self.title

    def getTitle(self) -> Optional[str]:
        return self.title

    def get_owner(self) -> Optional[str]:
        return self.owner

    def getOwner(self) -> Optional[str]:
        return self.owner

    def get_runbook(self) -> Optional[str]:
        return self.runbook

    def getRunbook(self) -> Optional[str]:
        return self.runbook

    def get(self, name: str) -> Optional[str]:
        if name is None:
            raise ValueError("name must not be None")
        return self.fields.get(name)

    def get_fields(self) -> Mapping[str, str]:
        return self.fields

    def getFields(self) -> Mapping[str, str]:
        return self.fields


def _strip_comment(line: str) -> str:
    in_single = False
    in_double = False
    for idx, ch in enumerate(line):
        if ch == "'" and not in_double:
            in_single = not in_single
        elif ch == '"' and not in_single:
            in_double = not in_double
        elif ch == "#" and not in_single and not in_double:
            return line[:idx]
    return line


def _unquote(value: str) -> str:
    if len(value) >= 2:
        if (value.startswith('"') and value.endswith('"')) or (
            value.startswith("'") and value.endswith("'")
        ):
            return value[1:-1]
    return value


def parse_knowledge_base(yaml_content: str) -> dict[str, FingerprintKnowledge]:
    if yaml_content is None:
        raise ValueError("yaml_content must not be None")
    entries: dict[str, FingerprintKnowledge] = {}
    current_id: Optional[str] = None
    current_fields: dict[str, str] = {}

    def flush() -> None:
        if current_id is not None:
            entries[current_id] = FingerprintKnowledge(current_id, current_fields)

    for idx, raw_line in enumerate(yaml_content.splitlines(), start=1):
        line = _strip_comment(raw_line)
        if not line.strip():
            continue
        indented = line.startswith(" ") or line.startswith("\t")
        trimmed = line.strip()

        if not indented:
            if not trimmed.endswith(":") or len(trimmed) == 1:
                raise ValueError(f"Invalid knowledge base entry at line {idx}: {raw_line}")
            flush()
            current_id = _unquote(trimmed[:-1].strip())
            current_fields = {}
        else:
            if current_id is None:
                raise ValueError(f"Field declared before fingerprint ID at line {idx}")
            colon_idx = trimmed.find(":")
            if colon_idx <= 0:
                raise ValueError(f"Invalid knowledge base field at line {idx}: {raw_line}")
            key = trimmed[:colon_idx].strip()
            val = _unquote(trimmed[colon_idx + 1 :].strip())
            current_fields[key] = val

    flush()
    return entries


_DEFAULT_KNOWLEDGE_FILES = (
    "bugdna.yml",
    "bugdna.yaml",
    "bugdna-fingerprints.yml",
    "bugdna-fingerprints.yaml",
)

_loaded_knowledge_base: Optional[dict[str, FingerprintKnowledge]] = None


def read_knowledge_base(path: Union[str, Path]) -> dict[str, FingerprintKnowledge]:
    if path is None:
        raise ValueError("path must not be None")
    content = Path(path).read_text(encoding="utf-8")
    return parse_knowledge_base(content)


def load_knowledge_base(
    source: Union[str, Path, Mapping[str, FingerprintKnowledge]],
) -> None:
    global _loaded_knowledge_base
    if source is None:
        raise ValueError("source must not be None")
    if isinstance(source, (str, Path)):
        _loaded_knowledge_base = read_knowledge_base(source)
    elif isinstance(source, Mapping):
        _loaded_knowledge_base = dict(source)
    else:
        raise TypeError("Unsupported knowledge base source")


def clear_knowledge_base_for_testing() -> None:
    global _loaded_knowledge_base
    _loaded_knowledge_base = None


def _discover_default_knowledge_base() -> dict[str, FingerprintKnowledge]:
    configured = os.environ.get("BUGDNA_KNOWLEDGE_PATH")
    if configured and configured.strip():
        return read_knowledge_base(configured.strip())
    for file_name in _DEFAULT_KNOWLEDGE_FILES:
        file_path = Path(file_name)
        if file_path.is_file():
            return read_knowledge_base(file_path)
    return {}


def lookup(fingerprint_id: str) -> Optional[FingerprintKnowledge]:
    global _loaded_knowledge_base
    if not fingerprint_id:
        raise ValueError("id must not be empty")
    if _loaded_knowledge_base is None:
        _loaded_knowledge_base = _discover_default_knowledge_base()
    return _loaded_knowledge_base.get(fingerprint_id)
