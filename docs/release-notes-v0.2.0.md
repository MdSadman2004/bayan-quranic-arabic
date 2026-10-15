# Bayan v0.2.0 — gamified feature layer + offline everywhere

Bayan is a Duolingo-style micro-learning prototype for **Quranic Arabic** — in **English and বাংলা**.
One shared curriculum drives two clients in this repo: a dependency-free **web app** and a native
**Kotlin + Jetpack Compose Android app** (`com.mdsadman.bayan` v0.2.0, versionCode 2).

## What's new in this release

**Gamified feature layer** (one shared payload, `curriculum/features.json` → web + Android):

- Noor flow combo (2×→8× XP multiplier + pitch ladder on feedback tones)
- Barakah chests with rarity tiers (Root Relics / Poetry Scrolls / Streak Shields)
- Sarf root-forge card inside lessons (root + mould → forged word)
- Waswasah whisper boss — 45-second deflector; full clear drops a streak shield
- Ṣalāh & rites surface: step-through prayer simulator, 90-second pre-prayer primer keyed to the
  approximate hour, wuḍūʾ/adhan roots, Friday Surah-al-Kahf exploration, ṣawm listening drills,
  zakāh/ḥajj roots
- Ḥalaqah circle (device-local companions, shared lantern, duʿāʾ shield) + illumination map
  (mastered roots light the collected verses that carry them)
- Etymology "wisdom seeds" on first mastery of a seeded root; permanence notes on completion
- Lesson-entry loaders (root sprout / mushaf shimmer) and a time-of-day sky gradient

**Curriculum**: 10 units × 3 lessons, 137 items, 35 verses, 35 context cards, 87 roots
(previously 8 units / 24 lessons / 115 items / 27 verses / 76 roots).

**Offline**: the web client ships a service worker with a hash-verified asset manifest
(`python tools/build_offline.py`) — the whole app, fonts, data and feature code work with the
network off. Android ships curriculum + features as assets and needs zero network.

**Fixes** (found by driving the real UI, each with a regression test where testable):

- Forge card could not be continued after answering — missing click binding + overly strict
  continue guard (`web/js/app.js`).
- Lesson completion crashed with `F.flowBonus is not a function` — renamed export; the
  completion screen now applies the multiplier through `flowMultiplier`/`flowXp`. A new
  `ui-contract` test now asserts every `F.*` call in the UI resolves against the feature
  engine's real exports.
- Validator: bogus verse refs (e.g. `112:999`) now rejected with a 286-ayah bound.

## Verified for this tag

- **Web, end-to-end in a real browser (local fixture)**: onboarding (EN + বাংলা) → lesson with the
  injected forge card → chest + 8× flow + +208/+192 XP completion → etymology seed card
  (permanence / ḥ-m-d) → boss 8/8 (48 XP, streak shield; timeout path exercised too) →
  illumination map 12/35 verses after two lessons → ḥalaqah member add + shield →
  rites surface in both languages; 0 console errors on the final pass.
- **Tests**: 57 Node tests (`node --test "tests/*.cjs"`) + 37 Python curriculum tests — all green.
- **Android**: `testDebugUnitTest`, `lintDebug`, `assembleDebug` all green on JDK 17 / Gradle 8.9;
  APK `app-debug.apk` (versionCode 2, versionName 0.2.0).
- **Not device-verified for this tag**: the phone used for v0.1.0 verification was disconnected
  during this build; the Android feature screens compile and pass lint/JVM tests but have not
  been run on physical hardware since the feature port. Treat on-device polish as pending.

Earlier device evidence for the base client (v0.1.0): installed and run on a Redmi 25062RN2DA
(HyperOS / Android 16) — cold start, warm deep links, scripted lesson completion, 0 crashes,
edge-to-edge insets verified by pixel probe.

## Install

- Android: `adb install -r app-debug.apk` (or download and open it on the phone).
- Web: GitHub Pages — https://mdsadman2004.github.io/bayan-quranic-arabic/

> **Prototype caveat.** Vocabulary order follows Quranic Arabic Corpus frequency data; context
> notes summarize classical asbāb al-nuzūl collections and well-attested ḥadīth. The feature
> layer's scholarly notes are pedagogical aids — have them reviewed before wider release.

MIT licensed. Built by Md Sadman Bin Masud.
