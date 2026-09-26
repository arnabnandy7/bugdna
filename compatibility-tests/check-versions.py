#!/usr/bin/env python3
"""Verifies that Java, TypeScript, and Python BugDNA packages share the same version."""

from __future__ import annotations

import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def get_maven_version() -> str:
    pom = ROOT / "pom.xml"
    tree = ET.parse(pom)
    root = tree.getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    version_el = root.find("m:version", ns)
    if version_el is None or not version_el.text:
        raise RuntimeError("Could not find <version> in root pom.xml")
    return version_el.text.strip()


def get_npm_version() -> str:
    pkg = json.loads((ROOT / "bugdna-js" / "package.json").read_text(encoding="utf-8"))
    return str(pkg["version"]).strip()


def get_python_version() -> str:
    pyproject = (ROOT / "bugdna-python" / "pyproject.toml").read_text(encoding="utf-8")
    match = re.search(r'^version\s*=\s*"([^"]+)"', pyproject, re.MULTILINE)
    if not match:
        raise RuntimeError("Could not find version in bugdna-python/pyproject.toml")
    return match.group(1).strip()


def main() -> int:
    java_ver = get_maven_version()
    js_ver = get_npm_version()
    py_ver = get_python_version()

    print(f"Java (pom.xml):                {java_ver}")
    print(f"TypeScript (package.json):     {js_ver}")
    print(f"Python (pyproject.toml):       {py_ver}")

    if len({java_ver, js_ver, py_ver}) != 1:
        print("ERROR: Version mismatch across ecosystems!", file=sys.stderr)
        return 1

    print(f"OK: All ecosystems aligned at version {java_ver}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
