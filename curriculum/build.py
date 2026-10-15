#!/usr/bin/env python3
"""Bayan curriculum build: validate unit files, merge, emit web + android payloads.

Usage: python curriculum/build.py
Outputs:
  dist/curriculum.json
  web/js/data.js
  android/app/src/main/assets/curriculum.json
"""
from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
CUR = ROOT / "curriculum"
AR = r"[\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]"
ROOT_RE = re.compile(rf"^{AR} {AR} {AR}$")

errors: list[str] = []
warnings: list[str] = []


def err(msg: str) -> None:
    errors.append(msg)


def load_json(path: pathlib.Path):
    try:
        with open(path, encoding="utf-8") as fh:
            return json.load(fh)
    except FileNotFoundError:
        err(f"missing file: {path}")
    except json.JSONDecodeError as exc:
        err(f"invalid JSON in {path}: {exc}")
    return None


def require(node: dict, fields: list[str], where: str) -> None:
    for f in fields:
        if f not in node or node[f] in (None, "", []):
            err(f"{where}: missing field '{f}'")


def check_text(node: dict, field: str, where: str) -> None:
    val = node.get(field)
    if isinstance(val, dict):
        require(val, ["en", "bn"], f"{where}.{field}")


def main() -> int:
    meta = load_json(CUR / "meta.json")
    if meta is None:
        print_errors()
        return 1

    units = []
    for uid in meta["unitOrder"]:
        unit = load_json(CUR / "units" / f"{uid}.json")
        if unit is None:
            continue
        if unit.get("id") != uid:
            err(f"unit file {uid}.json declares id '{unit.get('id')}'")
        units.append(unit)

    seen_ids: dict[str, str] = {}   # id -> where
    item_defs: dict[str, dict] = {}
    all_lessons = []

    def reserve(nid: str, where: str) -> None:
        if nid in seen_ids:
            err(f"duplicate id '{nid}' ({where} vs {seen_ids[nid]})")
        else:
            seen_ids[nid] = where

    for unit in units:
        where_u = f"unit {unit.get('id')}"
        require(unit, ["id", "num", "title", "subtitle", "lessons"], where_u)
        check_text(unit, "title", where_u)
        check_text(unit, "subtitle", where_u)
        for lesson in unit.get("lessons", []):
            where_l = f"{where_u}/{lesson.get('id')}"
            require(lesson, ["id", "title", "teaches", "items", "verses"], where_l)
            check_text(lesson, "title", where_l)
            reserve(lesson["id"], where_l)
            all_lessons.append((unit, lesson))

            for t in lesson.get("teaches", []):
                where_t = f"{where_l}/teach {t.get('id')}"
                require(t, ["id", "kind", "ar", "en", "bn"], where_t)
                if t.get("kind") not in ("word", "concept", "phrase"):
                    err(f"{where_t}: bad kind '{t.get('kind')}'")
                reserve(t["id"], where_t)

            for it in lesson.get("items", []):
                if "ref" in it:
                    continue  # resolved after all defs are known
                where_i = f"{where_l}/item {it.get('id')}"
                require(it, ["id", "ar", "en", "bn"], where_i)
                if "root" in it and not ROOT_RE.match(it["root"]):
                    err(f"{where_i}: root '{it['root']}' is not 3 space-separated letters")
                reserve(it["id"], where_i)
                item_defs[it["id"]] = it

            if len(lesson.get("items", [])) < 4:
                warnings.append(f"{where_l}: only {len(lesson.get('items', []))} items")

            for v in lesson.get("verses", []):
                where_v = f"{where_l}/verse {v.get('id')}"
                require(v, ["id", "ar", "tokens", "en", "bn", "ref"], where_v)
                reserve(v["id"], where_v)
                joined = " ".join(v.get("tokens", []))
                if joined != v.get("ar"):
                    err(f"{where_v}: tokens do not join to ar\n  tokens-joint: {joined}\n  ar: {v.get('ar')}")
                ctx = v.get("context")
                if ctx:
                    if ctx.get("kind") not in ("asbab", "story", "theme", "others"):
                        err(f"{where_v}: context kind '{ctx.get('kind')}'")
                    check_text(ctx, "title", where_v)
                    check_text(ctx, "body", where_v)

    # resolve refs
    for unit, lesson in all_lessons:
        for it in lesson.get("items", []):
            if "ref" in it and it["ref"] not in item_defs:
                err(f"lesson {lesson['id']}: ref '{it['ref']}' does not resolve")

    # stats
    n_items = len(item_defs)
    n_verses = sum(len(l[1].get("verses", [])) for l in all_lessons)
    n_teaches = sum(len(l[1].get("teaches", [])) for l in all_lessons)
    n_roots = len({i["root"] for i in item_defs.values() if i.get("root")})
    n_ctx = sum(
        1 for l in all_lessons for v in l[1].get("verses", []) if v.get("context")
    )

    if errors:
        print_errors()
        return 1

    merged = {
        "app": meta["app"],
        "version": meta.get("version", 1),
        "totalQuranWords": meta.get("totalQuranWords", 77429),
        "units": units,
    }

    (ROOT / "dist").mkdir(exist_ok=True)
    with open(ROOT / "dist" / "curriculum.json", "w", encoding="utf-8") as fh:
        json.dump(merged, fh, ensure_ascii=False, indent=1)

    js = "// generated by curriculum/build.py - do not edit\nwindow.BAYAN_CURRICULUM = "
    js += json.dumps(merged, ensure_ascii=False, separators=(",", ":")) + ";\n"
    (ROOT / "web" / "js").mkdir(parents=True, exist_ok=True)
    with open(ROOT / "web" / "js" / "data.js", "w", encoding="utf-8") as fh:
        fh.write(js)

    android_assets = ROOT / "android" / "app" / "src" / "main" / "assets"
    android_assets.mkdir(parents=True, exist_ok=True)
    with open(android_assets / "curriculum.json", "w", encoding="utf-8") as fh:
        json.dump(merged, fh, ensure_ascii=False, separators=(",", ":"))

    print(f"OK  units={len(units)}  lessons={len(all_lessons)}  teaches={n_teaches}")
    print(f"    items(unique)={n_items}  verses={n_verses}  contexts={n_ctx}  roots={n_roots}")
    for w in warnings:
        print(f"WARN {w}")
    print("wrote dist/curriculum.json, web/js/data.js, android assets/curriculum.json")
    return 0


def print_errors() -> None:
    for e in errors:
        print(f"ERROR {e}")
    print(f"-- {len(errors)} error(s)")


if __name__ == "__main__":
    sys.exit(main())
