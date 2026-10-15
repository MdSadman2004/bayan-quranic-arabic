#!/usr/bin/env python3
"""Android string-table contract test.

A raw key leaking into the UI (the rite screens once rendered "circle.members"
and "map.title" as literal text) happens when a screen asks for a key nobody
defined: the lookup falls back to the key itself. This test scans every key an
Android UI file can ask for — plain literals plus the runtime-built prefixes like
"chest." + kind — and asserts it resolves in English and Bangla, that both
languages cover the same keys, and that no value is empty.

The web client has the same guard in `tests/ui-contract.test.cjs`.
"""
from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ANDROID_UI = ROOT / "android/app/src/main/java/com/mdsadman/bayan"
STRINGS_KT = ANDROID_UI / "Strings.kt"

# Dotted literals that are not UI strings (asset filenames).
NOT_STRINGS = {"curriculum.json", "features.json", "data.js", "index.html"}

# Prefixes built at runtime in the screens: every value they take must exist.
PREFIXES = {"chest.": ["relic", "scroll", "shield"], "theme.": ["auto", "light", "dark"]}


def maps() -> dict[str, dict[str, str]]:
    src = STRINGS_KT.read_text(encoding="utf-8")
    out: dict[str, dict[str, str]] = {}
    for name in ("EN", "BN"):
        start = src.index(f"private val {name} = mapOf(")
        end = src.index("\n    )", start)
        body = src[start:end]
        out[name] = {
            key: value
            for key, value in re.findall(r'"([^"]+)"\s+to\s+"((?:[^"\\]|\\.)*)"', body)
        }
    return out


def keys_used() -> dict[str, set[str]]:
    used: dict[str, set[str]] = {}
    for path in sorted(ANDROID_UI.glob("*.kt")):
        if path.name == "Strings.kt":
            continue
        for match in re.finditer(r'"([a-z][A-Za-z0-9]*\.[A-Za-z0-9_.]*)"', path.read_text(encoding="utf-8")):
            key = match.group(1)
            if key in NOT_STRINGS:
                continue
            used.setdefault(key, set()).add(path.name)
    return used


class AndroidStringsTest(unittest.TestCase):
    def test_every_key_the_screens_ask_for_is_defined(self) -> None:
        defined = maps()["EN"]
        missing = {
            key: sorted(files)
            for key, files in keys_used().items()
            if not key.endswith(".") and key not in defined
        }
        self.assertEqual(missing, {}, f"Android screens ask for undefined keys: {missing}")

    def test_runtime_built_keys_exist(self) -> None:
        defined = maps()["EN"]
        for prefix, values in PREFIXES.items():
            for value in values:
                self.assertIn(prefix + value, defined, f"{prefix + value} must be defined")

    def test_both_languages_cover_the_same_keys(self) -> None:
        both = maps()
        english, bangla = set(both["EN"]), set(both["BN"])
        self.assertEqual(sorted(english - bangla), [], "keys missing from Bangla")
        self.assertEqual(sorted(bangla - english), [], "keys missing from English")

    def test_no_empty_translations(self) -> None:
        for lang, table in maps().items():
            empty = sorted(key for key, value in table.items() if not value.strip())
            self.assertEqual(empty, [], f"{lang} has empty strings: {empty}")

    def test_new_surface_copy_ships(self) -> None:
        # Placement + appearance: if any of these vanish the screens regress to
        # raw keys on a device, which is exactly the failure this guards.
        defined = maps()["EN"]
        required = [
            "placement.title", "placement.start", "placement.apply", "placement.undo",
            "placement.result", "placement.tierLabel", "placement.score", "placement.placed",
            "profile.theme", "profile.themeNote", "theme.auto", "theme.light", "theme.dark",
        ]
        self.assertEqual([k for k in required if k not in defined], [], "missing new-surface strings")


if __name__ == "__main__":
    unittest.main()
