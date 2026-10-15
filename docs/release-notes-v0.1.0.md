# Bayan v0.1.0 — Quranic Arabic micro-learning (prototype)

Bayan is a Duolingo-style micro-learning prototype for **Quranic Arabic** — in **English and বাংলা**.
One shared curriculum drives two clients in this repo: a dependency-free **web app** and a native
**Kotlin + Jetpack Compose Android app**.

## What's in this release

- `app-debug.apk` — installable Android debug build (package `com.mdsadman.bayan`, minSdk 26,
  targetSdk 35). Sideload with `adb install -r app-debug.apk` (enable "Install unknown apps" if
  installing from the phone).

## Content

- **8 units × 3 lessons**, ~115 vocabulary items, **27 fully vocalized Quranic verses**, each with
  a context card: *why it was revealed* (asbab al-nuzul), a story (e.g. Sulayman's letter to Sheba),
  or the theme. 76 root words.
- Vocabulary order follows the **Quranic Arabic Corpus** frequency data (top-100 words ≈ 55% of the
  text; 300–500 roots ≈ 80%).
- Graduation unit reads Al-Fātiḥah, Al-ʿAṣr and the opening of Āyat al-Kursī end-to-end.

## Learning loop

Teach → practice → read a real ayah → finish. Ten exercise types (both translation directions,
tap-the-pairs, root identification, word-bank sentence building, fill-the-blank, listening).
Gamification: XP, daily-goal ring, streak, hearts (3-strikes), combo bonus, gems, 12 achievements,
unit bonus, "Quranic coverage" estimate. Retention: simplified SM-2 spaced repetition
(0/1/2/4/8/16-day intervals) with flagged-item priority in the review queue.

## Verified for this tag

- `python curriculum/build.py` — schema + token-join validation (8 units, 24 lessons, 115 items,
  27 verses, 27 context cards) and payload emission for both clients.
- Web client driven end-to-end in a real browser: onboarding → lesson → completion → streak/XP →
  review queue → profile; wrong-answer, out-of-hearts, retry and persistence paths exercised.
- Android: `assembleDebug` build (Gradle 8.9, AGP 8.4.0, Kotlin 2.0.21) — APK's package, launcher
  activity and bundled `assets/curriculum.json` verified with `aapt`/`unzip`.
- **Not yet verified:** Android install/launch on a physical device (no device was attached at
  build time). It's a study prototype — verify scholarly content before wider use.

MIT licensed. Built by Md Sadman Bin Masud.
