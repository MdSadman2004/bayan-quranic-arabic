# Bayan — Quranic Arabic micro-learning (prototype)

Micro-learning app for **Quranic Arabic** in the shape of a Duolingo-style path:
real Quranic verses only, frequency-ordered vocabulary, the Arabic root system, and
spaced review — in **English and বাংলা**.
One shared curriculum drives **two clients**: a dependency-free **web app** and a native
**Kotlin + Jetpack Compose Android app**.

> **Status: prototype.** Vocabulary order follows the Quranic Arabic Corpus frequency
> data; context notes summarize classical asbāb al-nuzūl collections and well-attested
> hadith. Verify scholarly content before wider release.

## Screenshots (web client)

| Home path | Concept card | Translate exercise |
|---|---|---|
| ![Home](docs/screens/web-home.png) | ![Teach](docs/screens/web-teach.png) | ![Build](docs/screens/web-build.png) |

| Rites (ṣalāh simulator) | Ḥalaqah + illumination map | Whisper boss |
|---|---|---|
| ![Rites](docs/screens/web-rites.png) | ![Circle](docs/screens/web-circle.png) | ![Boss](docs/screens/web-boss.png) |

## Clients

| Client | Path | Run |
|---|---|---|
| Web | `web/index.html` | open it directly, or serve `web/` statically |
| Android | `android/` | `assembleDebug` with JDK 17 + Gradle 8.9 (see below) |

## Build the data

```bash
python curriculum/build.py     # validates + emits web/js/data.js and android assets
```

## Run the web app

```bash
cd web && python -m http.server 8890
# then open http://127.0.0.1:8890  (file:// also works)
```

## Build the Android APK

```bash
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.20.8-hotspot"
cd android
"D:/Apps/gradle-8.9/bin/gradle.bat" assembleDebug
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```

Install (device connected): `adb install -r app-debug.apk`

## What's inside

- `curriculum/` — the content: **10 units × 3 lessons**, 137 vocabulary items,
  35 fully vocalized Quranic verses with asbab al-nuzul / story / theme context cards,
  87 root words. Units 1→10 go from THIS/THAT and nominal sentences to full-surah
  reading (Al-Fātiḥah, Al-ʿAṣr, Āyat al-Kursī).
- `web/` — static web client, no build step, `localStorage` persistence, Amiri +
  Fraunces + Noto Serif Bengali bundled locally. Arabic TTS via Web Speech API when an
  Arabic voice exists (listen exercises hide themselves otherwise).
- `android/` — Kotlin + Jetpack Compose client (AGP 8.4.0 / Kotlin 2.0.21 /
  Compose BOM 2024.12.01, minSdk 26, targetSdk 35). Same engine ported 1:1,
  `org.json` parsing from assets, `SharedPreferences` persistence, Android TTS.
- `DESIGN.md` — pedagogy, data schema, lesson-compiler spec, gamification math.

Shared learning loop: XP, daily-goal ring, streak flame, hearts (3-strikes per lesson),
combo bonus, gems, 12 achievements, unit-complete bonus, and a "Quranic coverage"
estimate. Retention: simplified SM-2 spaced repetition (intervals 0/1/2/4/8/16 days)
with a review queue that prioritizes flagged (missed) items, plus deliberate
interleaving — later lessons reference earlier items.

Exercise types: teach cards, asbab/story/theme context cards, read cards with
hidden translation, multiple choice both directions, tap-the-pairs, root
identification, word-bank sentence building, fill-the-blank, and listen (TTS).

## Gamified feature layer

A second layer sits on top of the lesson loop, driven by `curriculum/features.json`
(one payload → web + Android):

- **Noor flow combo** — consecutive fast answers raise a 2×→8× XP multiplier and
  step the synthesized feedback tone up a musical ladder.
- **Barakah chests** — lessons can drop a chest (Root Relic / Poetry Scroll /
  Streak Shield) with rarity tiers; loot is collectible.
- **Sarf root-forge** — a crafting-table card inside lessons: drag a root onto a
  mould (فَاعِل، مَفْعُول، فَعِيل) and pick the word it forges.
- **Waswasah whisper boss** — a 45-second deflector round: false glosses descend,
  tap the true meaning; a full clear drops a streak shield.
- **Ṣalāh & rites** — a step-through prayer simulator, a 90-second pre-prayer
  primer keyed to the approximate hour, wuḍūʾ/adhan terms, a Friday Surah-al-Kahf
  exploration, ṣawm listening drills, and zakāh/ḥajj roots.
- **Ḥalaqah circle** — a device-local study circle (up to four companions) whose
  shared lantern dims on missed days and can be relit with a duʿāʾ shield.
- **Illumination map** — every mastered root lights the collected verses that
  carry it, showing how far a small root list reaches.
- **Wisdom seeds** — etymology cards (dunyā, ʿaql, qalb, ṣabr, shukr…) surface
  when their root is first mastered; noun-vs-verb permanence notes ride on
  completion screens.
- **Loaders & sky** — lesson-entry animations (root sprout, mushaf shimmer) and a
  time-of-day sky gradient (dawn → night), plus optional breath-paced entry.

## Offline & backup

- **Web**: a service worker caches the whole shell, fonts, data and feature code
  (hash-verified manifest from `python tools/build_offline.py`), so the app opens
  and lessons run with the network fully off. Progress exports/imports as a
  portable JSON file; the store keeps rotating backups and never overwrites a
  corrupt primary with a fresh state.
- **Android**: curriculum and features ship as assets — every reading, exercise
  and feature works with no network at all. Listening exercises appear only when
  an offline Arabic TTS voice is installed.

## Placement, appearance and feel

- **Placement test** — a learner who already reads Arabic takes a ~2-minute adaptive
  check (binary search over the ten tiers, four questions per probe). Units below the
  tier the learner demonstrates are marked *placed*: they unlock the path without
  granting XP or stars and never overwrite a lesson finished by hand. Undoable in one
  tap; entirely on-device.
- **Dark mode** — Auto (follows the device) / Light / Dark, stored with progress. The
  dark palette is the same warm world after maghrib: lamp-lit ink and sand with a
  lifted terracotta, plus night variants of the sky gradients.
- **Our own glyph family** — no stock fire, gem, person or star icons and no emoji in
  the UI. One 24×24 geometry is shared by both clients: khatim star (streak), dīnār
  seal (gems), stair path (learn), echo loop (practice), mihrab arch (rites), ḥalaqah
  ring (circle), qalam nib (profile), with sprout/spark/lamp/scroll marks for
  achievements.
- **Press feedback** — every press plays a short synthesized click (offline, no audio
  assets) and a light haptic tick; both respect the sound setting.

## Sources

- Vocabulary/frequency: Quranic Arabic Corpus (top-100 words ≈ 55% of the text;
  300–500 roots ≈ 80%).
- Context: classical asbāb al-nuzūl collections (al-Wāḥidī, Ibn Kathīr summaries),
  surah introductions, and well-attested ṣaḥīḥ hadith, phrased as short reports.
- Translations: English and Bangla study glosses by the author — not a substitute
  for a published tarjama.

MIT licensed. Built by Md Sadman Bin Masud.
