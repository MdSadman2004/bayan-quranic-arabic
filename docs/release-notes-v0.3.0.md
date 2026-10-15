# Bayan v0.3.0 — placement, dark mode, tactile press, a glyph family of our own

Bayan is a micro-learning prototype for **Quranic Arabic** — in **English and বাংলা**. One shared
curriculum drives two clients: a dependency-free **web app** and a native **Kotlin + Compose Android
app** (`com.mdsadman.bayan`, versionCode 3).

## What's new

**Placement test — the app meets you where you already are.** If a learner already reads Arabic, a
short adaptive check finds the tier they can start from. It binary-searches the ten unit tiers,
four questions per probe (~12 questions, ~2 minutes), and then marks the units below the tier as
*placed* — they unlock the path without granting XP or stars, and a lesson you finished yourself is
never rewritten. Fully on-device, and the whole thing is one tap to undo. Entry: a home banner, the
profile screen, or `--es screen placement`.

**Dark mode.** The same warm world after maghrib: lamp-lit ink and sand, with terracotta lifted so
it still reads on a dark ground. Three settings — Auto (follows the device), Light, Dark — stored
with your progress and available from the profile screen. Sky gradients have night variants.

**Bayan's own glyph family.** The streak flame and the faceted gem are gone, along with the
achievement emoji and the stock Material tab icons. In their place: a drawn family sharing one
24×24 geometry across web and Android — an eight-point *khatim* star for the streak, a *dīnār* seal
for gems, a stair path (learn), an echo loop (practice), a mihrab arch (rites), a ḥalaqah ring
(circle), a qalam nib (profile), plus sprout, spark, lamp and scroll marks for achievements.

**Press feedback everywhere.** Every press answers back: a short synthesized click (offline, no
assets) and a light haptic tick on Android, a click plus a short vibration on web where supported.
Both follow the existing sound setting.

**Stronger button contrast.** Secondary and outlined buttons now carry the accent tint and an accent
border with full-ink labels instead of washing out in `ink-soft`, and accent buttons use a deep
ink label in dark mode.

## Fixed

- **Android startup crash (device-found).** `ToneFeedback.init()` built an `AudioTrack` with a
  buffer size a Samsung/Android 16 device rejects, and it ran unguarded in `onCreate` — the app
  died on launch while every Gradle task reported success. The tone track now derives its buffer
  from `getMinBufferSize`, stays frame-aligned, and degrades to silence instead of throwing.
- Placement state and the appearance setting now round-trip through the portable store (lesson
  `placed` flags, `placedTier`, `placed`, `theme`), with the same validation rules as the rest of
  v1 progress.
- Lesson `placed` flags no longer disappear on reload.

## Verified for this tag

- **Web, driven end-to-end in a real browser:** sign-in → placement banner → adaptive check probing
  tiers 4 → 7 → 8 → 9 (12 questions) → "You placed at Unit 10" → **apply** (`placedTier 9`, 27
  lessons placed, XP 0, gems 0, stars 0, path resumes at `u10l1`) → **undo** (back to `u1l1`).
  Dark mode: toggled from the profile, `data-theme="dark"`, body computed background `rgb(23,20,15)`,
  persisted across reload. Zero console errors.
- **Tests:** 65 Node (`node --test "tests/*.cjs"`) + 37 Python curriculum tests — green. Android
  `testDebugUnitTest` + `lintDebug` + `assembleDebug` — green.
- **Android on a real device (Samsung SM-A075F, Android 16):** installs, launches (`Status: ok`,
  activity resumed), **0 FATAL exceptions**; lesson deep-link run produced 168 XP (= 21 × the 8×
  flow multiplier), SRS due exactly +1 day, and a chest drop persisted
  (`collection:["shield_streak"]`, `shields:1`, lantern 0.5); `theme:dark` persisted to
  `shared_prefs` and the rendered screen reads `#17140F`.

## Install

- Android: `adb install -r app-debug.apk` (or download and open it on the phone).
- Web: https://mdsadman2004.github.io/bayan-quranic-arabic/

> **Prototype caveat.** Vocabulary order follows Quranic Arabic Corpus frequency data; context notes
> summarize classical asbāb al-nuzūl collections and well-attested ḥadīth. The feature layer's
> scholarly notes are pedagogical aids — have them reviewed before wider release.

MIT licensed. Built by Md Sadman Bin Masud.
