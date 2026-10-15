# Bayan — Quranic Arabic micro-learning (prototype)

A Duolingo-style **micro-learning** app for **Quranic Arabic**, delivered as **one shared
curriculum** driving **two clients**:

- `web/` — static web app (no build step, opens from `file://` or any static server)
- `android/` — native Kotlin + Jetpack Compose app (same engine, same data)

Goal: take a learner from zero → reading and understanding real Quranic text, in small
daily sessions (3–5 min), through **frequency-first vocabulary**, **the Arabic root
system**, and **only genuine Quranic verses** — each with its background (asbab al-nuzul,
story context, or theme). Interface and translations in **English and Bangla**.

## Design principles

1. **Micro loop, macro map.** Every lesson is one 3–5 minute loop (~15–20 cards): teach →
   practice → read a real ayah → finish. The home path is the macro map (8 units × 3
   lessons + spaced review).
2. **Frequency first.** Vocabulary order follows the Quranic Arabic Corpus frequency data
   (top-100 words = ~55% of the text; 300–500 roots ≈ 80%). Items carry occurrence counts.
3. **Roots from day one.** Words are introduced with their 3-letter root; a `root` exercise
   asks the learner to identify roots, so word families unlock for free.
4. **Only real Quran, with context.** Examples are Quranic verses (fully vocalized).
   Verses are introduced with a context card: *why it was revealed* (asbab al-nuzul),
   a *story* (e.g. Sulayman's letter to Sheba), or the *theme*. Graduation unit reads
   Al-Fatiha, Al-Asr and Ayat al-Kursi end-to-end.
5. **Retention by construction.** A simplified SM-2-style spaced repetition tracks every
   item (levels 0–5, intervals 0/1/2/4/8/16 days); a review queue serves due items;
   wrong answers drop a level and are re-queued first. Lessons also reference earlier
   items (`ref`), so later lessons interleave review by design.
6. **Minimal, warm, gamified.** Warm sand paper / ink neutrals, clay-terracotta accent,
   hairline borders, serif display (Fraunces) + Amiri for Arabic. Gamification layer:
   XP, daily-goal ring, streak flame, hearts (3-strikes per lesson), combo bonus,
   gems, unit chest, 12 achievements, and a "Quran coverage" estimate stat.

## Curriculum map (8 units × 3 lessons)

| # | Unit | Focus | Anchor verses |
|---|------|-------|---------------|
| 1 | Foundations | THIS/THAT, definite article, nominal sentence, praise & mercy words | 112:1, 1:2–3 |
| 2 | The Possessive Chain | iḍāfa (X of Y), بِسْمِ, attached pronouns preview | 1:4, 27:30, 1:5 |
| 3 | He Said, They Said | past tense + ـوا (they), كَانَ, قَدْ | 103:3, 112:3–4, 23:1 |
| 4 | The Present | imperfect يَفْعَلُ, لا النافية, participles | 2:286, 2:2, 55:1–4 |
| 5 | Roots & Families | root system, families ك-ت-ب / ع-ل-م / ر-ح-م / أ-م-ن | 96:1, 6:12, 106:3–4 |
| 6 | Particles | إِنَّ, قَدْ, مَا/لَا, هَلْ + إِلَّا | 108:1–3, 103:1–2, 55:60, 80:1–2 |
| 7 | Carrying the Message | attached pronouns, command forms, أَرْسَلْنَا | 94:5–6, 2:152, 21:107 |
| 8 | Reading the Message | full-surah reading + retention | 1:1–7, 103:1–3, 2:255 |

Context sources: classical asbab al-nuzul collections (al-Wāḥidī / Ibn Kathīr summaries),
surah introductions, and well-attested hadith (Bukhārī/Muslim) — all phrased as short
classical reports, never as first-person certainty.

## Data schema (`curriculum/`)

- `meta.json` — unit order, app name, etc.
- `units/uN.json` — one unit per file:
  - `lessons[].teaches[]` — concept/word intro cards: `{id, kind: word|concept|phrase, ar, en, bn, note?{en,bn}}`
  - `lessons[].items[]` — practiceable vocabulary: `{id, ar, en, bn, root?, occ?, fam?, note?}`; an
    item may be `{ref: "<itemId>"}` to re-use an earlier item (interleaved review).
  - `lessons[].verses[]` — Quranic text: `{id, ar, tokens[], en, bn, ref, context?{kind: asbab|story|theme|others, title{en,bn}, body{en,bn}}}`
    (`" ".join(tokens)` must equal `ar` — enforced by the build script.)
- `build.py` — validates everything and emits:
  - `dist/curriculum.json` (canonical merged)
  - `web/js/data.js` (`window.BAYAN_CURRICULUM = …`)
  - `android/app/src/main/assets/curriculum.json`

Run: `python curriculum/build.py`

## Lesson compiler (shared engine logic)

`compile(lesson, state)` → card list:
teach → MC (AR→meaning) → MC (meaning→AR) → tap-match board → root exercise → verse:
context card → read card (translation hidden) → build-from-tiles → fill-the-blank →
listen (TTS) → remaining items → completion card. Review sessions compile the same way
from due SRS items only (no teaches). Distractors are drawn from other items of the same
unit, so wrong options are same-domain.

## Gamification math

- XP per lesson: `10 + round(5 × accuracy)` + perfect bonus 5 + combo bonus
  `min(floor(maxCombo/5),3) × 2`.
- Hearts: 5 per lesson; wrong answer −1; 0 → lesson ends (retry any time). Reset after
  any completed lesson or 15 min.
- Streak: +1 on first completed activity of a day (lesson or review); resets if a day is
  missed. Daily goal: 30 XP (ring on Home).
- Gems: +1 per lesson, +2 extra if perfect. Achievements: 12 (see `engine.js`).
- Coverage: `Σ occ of learned items / 77,429 words` — labelled **estimate**.

## Clients

### Web (`web/`)
`index.html` + `css/style.css` + `js/{data,engine,audio,app}.js`. No dependencies, no
build step; state in `localStorage` (`bayan.v1`). Arabic TTS via Web Speech API when an
Arabic voice exists; WebAudio chirps for feedback. Dev hook `window.__bayan` exposes the
current card + correct index for scripted verification.

### Android (`android/`)
Kotlin + Compose (AGP 8.4.0, Kotlin 2.0.21, Compose BOM 2024.12.01, minSdk 26,
compileSdk 35 — mirrors the proven Atlas toolchain on this machine). Same engine ported
to `Engine.kt`; curriculum parsed from assets with `org.json`; state in
`SharedPreferences`. Arabic TTS via `android.speech.tts.TextToSpeech`.
Debug deep-links for verification: `--es screen lesson --es lesson u1l1 [--ez auto true]`.

## Verification

- `python curriculum/build.py` — schema + token-join validation, prints counts.
- Web: driven in a real browser session end-to-end (onboarding → lesson → completion →
  streak/XP update → review queue → profile), assertions on DOM + state.
- Android: `gradle assembleDebug` + lint; device install/launch when a device is attached.
