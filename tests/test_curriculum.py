"""Deterministic unit tests for the Bayan curriculum validator.

Run:  python -m unittest discover -s tests -p "test_*.py" -v

Fixtures are in-memory dicts passed to validate_curriculum(), so a malformed
fixture can never corrupt the shipped corpus and no error state leaks between
cases.
"""
from __future__ import annotations

import json
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "curriculum"))

from build import load_curriculum, render_outputs, validate_curriculum  # noqa: E402

ROOT_ERROR = "units[0].lessons[0].items[0].root: expected 3 space-separated Arabic letters"


def meta(unit_order=("u1",)):
    return {
        "app": {"name": "Bayan", "nameAr": "بَيَان", "tagline": {"en": "T", "bn": "T"}},
        "version": 1,
        "totalQuranWords": 77429,
        "unitOrder": list(unit_order),
    }


def item(iid="u1i1", **over):
    node = {"id": iid, "ar": "عِلْم", "en": "knowledge", "bn": "জ্ঞান", "root": "ع ل م"}
    node.update(over)
    return node


def verse(vid="u1v1", **over):
    node = {
        "id": vid, "ar": "قُلْ هُوَ اللَّهُ", "tokens": ["قُلْ", "هُوَ", "اللَّهُ"],
        "en": "Say: He is Allah", "bn": "বলুন: তিনি আল্লাহ", "ref": "Al-Ikhlas 112:1",
    }
    node.update(over)
    return node


def lesson(lid="u1l1", **over):
    node = {
        "id": lid,
        "title": {"en": "Lesson", "bn": "পাঠ"},
        "teaches": [{"id": lid + "t1", "kind": "word", "ar": "عِلْم", "en": "knowledge", "bn": "জ্ঞান"}],
        "items": [item("u1i1"), item("u1i2"), item("u1i3"), item("u1i4")],
        "verses": [verse("u1v1")],
    }
    node.update(over)
    return node


def unit(uid="u1", **over):
    node = {
        "id": uid, "num": 1,
        "title": {"en": "Unit", "bn": "ইউনিট"},
        "subtitle": {"en": "Sub", "bn": "উপ"},
        "lessons": [lesson("u1l1")],
    }
    node.update(over)
    return node


def run(m, units):
    return validate_curriculum(m, units)


class ValidCorpus(unittest.TestCase):
    def test_valid_fixture(self):
        res = run(meta(), [unit()])
        self.assertEqual([], res.errors)
        self.assertTrue(res.valid)
        self.assertEqual(1, res.stats["units"])
        self.assertEqual(1, res.stats["lessons"])
        self.assertEqual(1, res.stats["verses"])
        self.assertEqual(4, res.stats["items"])
        self.assertEqual(1, res.stats["roots"])

    def test_shipped_corpus_valid_and_numbered_in_order(self):
        res = load_curriculum(ROOT / "curriculum")
        self.assertEqual([], res.errors, res.errors)
        self.assertTrue(res.valid)
        declared = json.loads((ROOT / "curriculum" / "meta.json").read_text(encoding="utf-8"))["unitOrder"]
        self.assertEqual(declared, [u["id"] for u in res.corpus["units"]])
        for index, u in enumerate(res.corpus["units"]):
            self.assertEqual(index + 1, u["num"], u["id"])

    def test_shipped_corpus_verses_join(self):
        res = load_curriculum(ROOT / "curriculum")
        checked = 0
        for u in res.corpus["units"]:
            for lesson_node in u["lessons"]:
                for v in lesson_node["verses"]:
                    self.assertEqual(" ".join(v["tokens"]), v["ar"])
                    checked += 1
        self.assertGreater(checked, 0)


class MetaShape(unittest.TestCase):
    def test_missing_app(self):
        m = meta()
        del m["app"]
        self.assertIn("meta.app: expected an object", run(m, [unit()]).errors)

    def test_bad_total_words(self):
        m = meta()
        m["totalQuranWords"] = 0
        self.assertIn("meta.totalQuranWords: expected a positive integer (not boolean)",
                      run(m, [unit()]).errors)

    def test_boolean_is_not_a_number(self):
        m = meta()
        m["version"] = True
        self.assertIn("meta.version: expected a positive integer (not boolean)",
                      run(m, [unit()]).errors)

    def test_unit_order_duplicate(self):
        errors = run(meta(("u1", "u1")), [unit(), unit("u1")]).errors
        self.assertTrue([e for e in errors if "duplicate unitOrder id 'u1'" in e], errors)

    def test_unit_order_path_rejected(self):
        self.assertIn("meta.unitOrder[0]: expected a unit id such as 'u1' (no paths)",
                      run(meta(("../u1",)), [unit("u1")]).errors)

    def test_unit_count_mismatch(self):
        self.assertIn("units: expected 1 units from unitOrder; got 0", run(meta(), []).errors)

    def test_unit_id_must_match_order(self):
        self.assertIn("units[0].id: expected 'u1' from unitOrder, got 'u2'",
                      run(meta(), [unit("u2")]).errors)

    def test_unit_number_sequence(self):
        wrong = unit()
        wrong["num"] = 3
        self.assertIn("units[0].num: expected 1 in unitOrder sequence", run(meta(), [wrong]).errors)


class ItemShape(unittest.TestCase):
    def test_missing_translation(self):
        bad = item("u1i1")
        del bad["bn"]
        errors = run(meta(), [unit(lessons=[lesson(items=[bad, item("u1i2"), item("u1i3"), item("u1i4")])])]).errors
        self.assertIn("units[0].lessons[0].items[0].bn: expected a nonempty string", errors)

    def test_duplicate_id(self):
        dup = item("u1i1")
        errors = run(meta(), [unit(lessons=[lesson(items=[dup, dict(dup), item("u1i3"), item("u1i4")])])]).errors
        self.assertTrue([e for e in errors if "duplicate id 'u1i1'" in e], errors)

    def test_root_must_be_three_arabic_letters(self):
        errors = run(meta(), [unit(lessons=[lesson(items=[item("u1i1", root="أ و")])])]).errors
        self.assertIn(ROOT_ERROR, errors)

    def test_root_rejects_latin_diacritics_and_extra_letters(self):
        for bad in ("a b c", "عِلْ م", "ع ل م ن"):
            errors = run(meta(), [unit(lessons=[lesson(items=[item("u1i1", root=bad)])])]).errors
            self.assertIn(ROOT_ERROR, errors, bad)

    def test_occ_must_be_positive(self):
        errors = run(meta(), [unit(lessons=[lesson(items=[item("u1i1", occ=0)])])]).errors
        self.assertIn("units[0].lessons[0].items[0].occ: expected a positive integer (not boolean)", errors)

    def test_ref_must_resolve(self):
        errors = run(meta(), [unit(lessons=[lesson(items=[{"ref": "nope"}, item("u1i2"), item("u1i3"), item("u1i4")])])]).errors
        self.assertIn("units[0].lessons[0].items[0]: item ref 'nope' does not resolve to an item definition", errors)

    def test_forward_ref_rejected(self):
        # The ref node comes first: clients build their index in one pass, so a ref
        # may only point at an item defined earlier in unitOrder/lesson/item order.
        forward = [{"ref": "u1i2"}, item("u1i2"), item("u1i3"), item("u1i4")]
        errors = run(meta(), [unit(lessons=[lesson(items=forward)])]).errors
        self.assertTrue([e for e in errors if "forward item ref 'u1i2'" in e], errors)

    def test_backward_ref_accepted(self):
        backwards = [item("u1i1"), {"ref": "u1i1"}, item("u1i3"), item("u1i4")]
        self.assertEqual([], run(meta(), [unit(lessons=[lesson(items=backwards)])]).errors)

    def test_ref_node_rejects_extra_fields(self):
        mixed = {"ref": "u1i1", "en": "x"}
        errors = run(meta(), [unit(lessons=[lesson(items=[item("u1i1"), mixed, item("u1i3"), item("u1i4")])])]).errors
        self.assertTrue([e for e in errors if "an item ref must contain only 'ref'" in e], errors)


class LessonShape(unittest.TestCase):
    def test_teach_kind(self):
        bad = lesson(teaches=[{"id": "t1", "kind": "phrase3", "ar": "ع", "en": "A", "bn": "A"}])
        self.assertIn("units[0].lessons[0].teaches[0].kind: expected word, concept or phrase",
                      run(meta(), [unit(lessons=[bad])]).errors)

    def test_empty_teaches(self):
        self.assertIn("units[0].lessons[0].teaches: expected a nonempty array",
                      run(meta(), [unit(lessons=[lesson(teaches=[])])]).errors)

    def test_duplicate_lesson_id(self):
        dup = lesson("u1l1")
        errors = run(meta(), [unit(lessons=[dup, dict(dup)])]).errors
        self.assertTrue([e for e in errors if "duplicate id 'u1l1'" in e], errors)

    def test_few_items_warns_not_errors(self):
        res = run(meta(), [unit(lessons=[lesson(items=[item("u1i1")])])])
        self.assertEqual([], res.errors)
        self.assertTrue([w for w in res.warnings if "only 1 items" in w])


class VerseShape(unittest.TestCase):
    def test_tokens_must_join_ar(self):
        self.assertIn("units[0].lessons[0].verses[0]: tokens do not join to ar with single spaces",
                      run(meta(), [unit(lessons=[lesson(verses=[verse(tokens=["قُلْ", "هُوَ"])])])]).errors)

    def test_empty_tokens(self):
        self.assertIn("units[0].lessons[0].verses[0].tokens: expected a nonempty array",
                      run(meta(), [unit(lessons=[lesson(verses=[verse(tokens=[])])])]).errors)

    def test_whitespace_inside_token(self):
        bad = verse(tokens=["قُلْ هُوَ", "اللَّهُ"], ar="قُلْ هُوَ اللَّهُ")
        self.assertIn("units[0].lessons[0].verses[0].tokens[0]: expected one token without whitespace",
                      run(meta(), [unit(lessons=[lesson(verses=[bad])])]).errors)

    def test_ref_out_of_range(self):
        errors = run(meta(), [unit(lessons=[lesson(verses=[verse(ref="Al-X 112:999")])])]).errors
        self.assertIn("units[0].lessons[0].verses[0].ref: expected a Quran source reference, e.g. Al-Ikhlas 112:1–4",
                      errors)

    def test_range_ref_allowed_missing_ref_rejected(self):
        good = verse(ref="Al-Ikhlas 112:1–4")
        no_ref = verse(vid="u1v2")
        del no_ref["ref"]
        errors = run(meta(), [unit(lessons=[lesson(verses=[good, no_ref])])]).errors
        self.assertIn("units[0].lessons[0].verses[1].ref: expected a nonempty string", errors)

    def test_context_kind(self):
        bad = verse(context={"kind": "not-a-kind", "title": {"en": "T", "bn": "T"}, "body": {"en": "B", "bn": "B"}})
        self.assertIn("units[0].lessons[0].verses[0].context.kind: expected asbab, story, theme or others",
                      run(meta(), [unit(lessons=[lesson(verses=[bad])])]).errors)

    def test_context_needs_bilingual_body(self):
        bad = verse(context={"kind": "theme", "title": {"en": "T", "bn": "T"}, "body": {"en": "B"}})
        self.assertIn("units[0].lessons[0].verses[0].context.body.bn: expected a nonempty string",
                      run(meta(), [unit(lessons=[lesson(verses=[bad])])]).errors)


class MalformedShapes(unittest.TestCase):
    def test_wrong_types_do_not_raise(self):
        res = run(meta(), [["not-a-unit"]])
        self.assertTrue(res.errors)
        self.assertFalse(res.valid)

    def test_meta_none_is_safe(self):
        self.assertEqual(["meta: expected an object"], run(None, []).errors)

    def test_no_corpus_on_failure(self):
        res = run(meta(), [])
        self.assertIsNone(res.corpus)
        self.assertFalse(res.valid)

    def test_errors_are_detached_between_calls(self):
        first = run(meta(), [])
        second = run(meta(), [unit()])
        self.assertTrue(first.errors)
        self.assertEqual([], second.errors)


class Outputs(unittest.TestCase):
    def test_three_surfaces_and_web_payload_shape(self):
        res = load_curriculum(ROOT / "curriculum")
        outputs = render_outputs(res.corpus, ROOT)
        self.assertEqual(4, len(outputs))
        web = outputs[ROOT / "web/js/data.js"]
        self.assertTrue(web.startswith("// generated by curriculum/build.py - do not edit\nwindow.BAYAN_CURRICULUM = "))
        payload = json.loads(web.split("=", 1)[1].rstrip().rstrip(";"))
        self.assertEqual(len(res.corpus["units"]), len(payload["units"]))
        self.assertIn(ROOT / "dist/curriculum.json", outputs)
        self.assertIn(ROOT / "android/app/src/main/assets/curriculum.json", outputs)

    def test_fourth_surface_android_features_payload(self):
        res = load_curriculum(ROOT / "curriculum")
        outputs = render_outputs(res.corpus, ROOT)
        path = ROOT / "android/app/src/main/assets/features.json"
        self.assertIn(path, outputs)
        features = json.loads(outputs[path])
        self.assertEqual(len(features["boss"]["waves"]), 8)
        self.assertEqual(len(features["rites"]["salah"]["steps"]), 7)


if __name__ == "__main__":
    unittest.main()
