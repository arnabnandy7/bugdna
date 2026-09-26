"""MkDocs hook: copy external markdown sources and rewrite their edit_uri."""

from pathlib import Path

# Maps docs-relative destination paths to their real repository paths.
_EXTERNAL_PAGES = {
    "specification.md": "specification/SPECIFICATION.md",
    "bugdna-js.md": "bugdna-js/README.md",
    "bugdna-python.md": "bugdna-python/README.md",
}

_DOCS_DIR = Path(__file__).resolve().parent.parent


def on_pre_build(config, **_kwargs):
    """Copy external repository markdown files into docs_dir before building."""
    docs_dir = Path(config["docs_dir"])
    repo_root = docs_dir.parent

    for dest_rel, src_rel in _EXTERNAL_PAGES.items():
        src_path = repo_root / src_rel
        dest_path = docs_dir / dest_rel
        content = src_path.read_text(encoding="utf-8")
        if dest_rel == "specification.md":
            content = content.replace(
                "#14-timeline--burst-detection",
                "#14-timeline-burst-detection",
            )
        dest_path.write_text(content, encoding="utf-8")


def on_page_context(context, page, config, **_kwargs):
    """Point edit links for copied pages to their canonical repository paths."""
    src = page.file.src_path.replace("\\", "/")
    real_path = _EXTERNAL_PAGES.get(src)
    if real_path is not None:
        repo_url = config.get("repo_url", "").rstrip("/")
        page.edit_url = f"{repo_url}/edit/main/{real_path}"
    return context


def on_shutdown(**_kwargs):
    """Remove transient copied markdown files from docs_dir on exit."""
    for dest_rel in _EXTERNAL_PAGES:
        dest_path = _DOCS_DIR / dest_rel
        if dest_path.exists():
            dest_path.unlink()
