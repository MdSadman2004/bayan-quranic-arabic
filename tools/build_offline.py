#!/usr/bin/env python3
"""Render the offline bundle: web/offline-assets.json + web/sw.js.

Usage: python tools/build_offline.py

Every bundled static asset is listed with a sha256 so the manifest doubles as an
integrity record, and the service worker is generated from tools/sw_template.js
with the file list and a content-derived version inlined — so the very first
visit can go offline without fetching a manifest.
"""
from __future__ import annotations

import hashlib
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
WEB = ROOT / "web"
TEMPLATE = ROOT / "tools" / "sw_template.js"
ASSETS = WEB / "offline-assets.json"
SW = WEB / "sw.js"
EXCLUDE = {"sw.js", "offline-assets.json"}
REQUIRED = ("index.html", "css/style.css", "js/app.js", "js/engine.js", "js/storage.js", "js/offline.js")


def sha256_file(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def collect() -> list[dict]:
    entries = []
    for path in sorted(WEB.rglob("*")):
        if not path.is_file() or path.name in EXCLUDE or "__pycache__" in path.parts:
            continue
        entries.append({"url": path.relative_to(WEB).as_posix(), "integrity": sha256_file(path)})
    return entries


def render_sw(entries: list[dict], version: str) -> str:
    template = TEMPLATE.read_text(encoding="utf-8")
    urls = json.dumps([e["url"] for e in entries], ensure_ascii=False, indent=2)
    rendered = template.replace("__FILES__", urls).replace("__VERSION__", version)
    if "__FILES__" in rendered or "__VERSION__" in rendered:
        raise SystemExit("template placeholders were not all substituted")
    return rendered


def main() -> int:
    if not TEMPLATE.exists():
        raise SystemExit(f"missing template: {TEMPLATE}")
    entries = collect()
    if not entries:
        raise SystemExit("no web assets found to cache")
    present = {e["url"] for e in entries}
    for required in REQUIRED:
        if required not in present:
            raise SystemExit(f"required asset missing from the offline set: {required}")
    version = hashlib.sha256("".join(e["integrity"] for e in entries).encode()).hexdigest()[:12]
    ASSETS.write_text(json.dumps(
        {"version": version, "cache": f"bayan-app-{version}", "files": entries},
        ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    SW.write_text(render_sw(entries, version), encoding="utf-8")
    total = sum((WEB / e["url"]).stat().st_size for e in entries)
    print(f"OK  offline files={len(entries)} bytes={total} cache_version={version}")
    print("wrote web/offline-assets.json, web/sw.js")
    return 0


if __name__ == "__main__":
    sys.exit(main())
