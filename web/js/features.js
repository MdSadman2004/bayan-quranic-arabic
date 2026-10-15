/* Bayan feature engine — flow combo, Barakah chests, Sarf forge, whisper boss,
 * wisdom seeds, rite modules, illumination map, ḥalaqah. Pure logic only (no
 * DOM), so every rule here is unit-testable and portable to the Android client.
 */
(function () {
  "use strict";
  const BAYAN = window.Bayan || {};
  const CUR = BAYAN.CUR || { units: [], features: {} };
  const F = CUR.features || {};
  const DAY = 86400000;
  const ARABIC_MARKS = /[\u064B-\u065F\u0670\u0640\u06D6-\u06ED]/g;

  const loot = (F.chests && F.chests.loot) || [];
  const rarities = (F.chests && F.chests.rarities) || [];
  const combo = F.combo || { windowSec: 8, steps: [2, 3, 4, 5, 6, 8], semitones: [0, 2, 4, 5, 7, 9] };
  const boss = F.boss || { seconds: 45, waves: [] };
  const seeds = (F.seeds && F.seeds.items) || [];
  const rites = F.rites || {};
  const whispers = F.whispers || [];
  const loaders = F.loaders || [];
  /* Rough hour boundaries for the pre-prayer primer. These are reminders, not a
   * prayer timetable — local times and seasons shift the real windows. */
  const WAQT_HOURS = [
    { upTo: 8, id: "fajr" },
    { upTo: 15, id: "dhuhr" },
    { upTo: 18, id: "asr" },
    { upTo: 20, id: "maghrib" },
    { upTo: 24, id: "isha" }
  ];

  function normalize(text) {
    return String(text == null ? "" : text).replace(ARABIC_MARKS, "").replace(/[\u0622\u0623\u0625]/g, "\u0627").toLowerCase();
  }

  /* --------------------------------------------------- 1. Noor flow combo */
  function flowMultiplier(streak) {
    const steps = combo.steps || [2];
    if (!Number.isFinite(streak) || streak <= 0) return 1;
    return steps[Math.min(streak - 1, steps.length - 1)];
  }
  function flowSemitones(streak) {
    const semis = combo.semitones || [0];
    if (!Number.isFinite(streak) || streak <= 0) return 0;
    return semis[Math.min(streak - 1, semis.length - 1)];
  }
  function flowXp(baseXp, streak) {
    const base = Number.isFinite(baseXp) ? baseXp : 0;
    return Math.round(base * flowMultiplier(streak));
  }
  function comboTick(comboState, correct, now, windowSec) {
    const win = (Number.isFinite(windowSec) ? windowSec : combo.windowSec || 8) * 1000;
    const state = comboState && Number.isFinite(comboState.streak)
      ? { streak: comboState.streak, lastAt: comboState.lastAt || 0 }
      : { streak: 0, lastAt: 0 };
    if (!correct) return { streak: 0, lastAt: now, multiplier: 1, semitones: 0 };
    const inWindow = now - state.lastAt <= win;
    const streak = inWindow ? state.streak + 1 : 1;
    return { streak: streak, lastAt: now, multiplier: flowMultiplier(streak), semitones: flowSemitones(streak) };
  }

  /* --------------------------------------------- 2. Barakah chests (loot) */
  function pickWeighted(list, rand) {
    const total = list.reduce((sum, item) => sum + (item.weight || 0), 0);
    if (total <= 0) return list[0] || null;
    let roll = rand() * total;
    for (const item of list) {
      roll -= item.weight || 0;
      if (roll <= 0) return item;
    }
    return list[list.length - 1];
  }
  function rollChest(run, rand) {
    // Nothing was answered, so nothing was earned — this also keeps a
    // zero-card run (for example a quit lesson) from paying out loot.
    if (!run || !run.total) return null;
    const chance = typeof (F.chests && F.chests.chance) === "number" ? F.chests.chance : 0;
    const perfect = run.mistakes === 0;
    const effective = Math.min(1, chance + (perfect ? 0.2 : 0));
    if (rand() > effective) return null;
    const rarity = pickWeighted(rarities, rand);
    const pool = loot.filter((entry) => !rarity || entry.rarity === rarity.id);
    const source = pool.length ? pool : loot;
    if (!source.length) return null;
    const entry = source[Math.floor(rand() * source.length)];
    return { id: entry.id, kind: entry.kind, rarity: entry.rarity, title: entry.title, body: entry.body, root: entry.root || null, verse: entry.verse || null };
  }
  function applyLoot(state, entry) {
    if (!state || !entry) return state;
    state.chests = state.chests || {};
    state.shields = state.shields || 0;
    if (entry.kind === "shield") state.shields += 1;
    else state.chests[entry.id] = (state.chests[entry.id] || 0) + 1;
    state.collection = state.collection || [];
    if (state.collection.indexOf(entry.id) < 0) state.collection.push(entry.id);
    return state;
  }
  function shieldAvailable(state) {
    return !!(state && state.shields > 0);
  }
  /* One missed day is covered by a shield instead of resetting the streak. */
  function resolveStreakGap(state, todayKey, yesterdayKey) {
    if (!state || !state.day) return { streak: state ? state.streak || 0 : 0, usedShield: false };
    if (state.day === todayKey || state.day === yesterdayKey) return { streak: state.streak || 0, usedShield: false };
    if (shieldAvailable(state)) {
      state.shields -= 1;
      return { streak: state.streak || 0, usedShield: true };
    }
    return { streak: 0, usedShield: false };
  }

  /* ---------------------------------------------------- 3. Sarf forge */
  function forgeRound(rand) {
    const moulds = (F.forge && F.forge.moulds) || [];
    if (!moulds.length) return null;
    const mould = moulds[Math.floor(rand() * moulds.length)];
    const examples = mould.examples || [];
    if (!examples.length) return null;
    const example = examples[Math.floor(rand() * examples.length)];
    const options = [example.word];
    for (const other of moulds) {
      if (other.id === mould.id) continue;
      for (const alternative of other.examples || []) {
        if (options.length >= 3) break;
        if (options.indexOf(alternative.word) < 0) options.push(alternative.word);
      }
    }
    for (const alternative of examples) {
      if (options.length >= 3) break;
      if (options.indexOf(alternative.word) < 0) options.push(alternative.word);
    }
    const order = options.map((word, index) => ({ word: word, index: index }))
      .sort(() => rand() - 0.5)
      .map((entry) => entry.word);
    return {
      mould: { id: mould.id, pattern: mould.pattern, role: mould.role },
      root: example.root,
      answer: order.indexOf(example.word),
      options: order,
      meaning: example.meaning,
      word: example.word
    };
  }

  /* ------------------------------------ 4. Whisper boss (timed deflector) */
  function bossQueue(rand, count) {
    const waves = (boss.waves || []).slice();
    for (let i = waves.length - 1; i > 0; i--) {
      const j = Math.floor(rand() * (i + 1));
      const tmp = waves[i]; waves[i] = waves[j]; waves[j] = tmp;
    }
    return waves.slice(0, Math.max(1, Math.min(count || waves.length, waves.length)));
  }
  function bossScore(answeredWaves, hits, secondsLeft) {
    const total = answeredWaves.length;
    const safeHits = Math.max(0, Math.min(hits, total));
    const accuracy = total ? safeHits / total : 0;
    const speedBonus = Math.round(Math.max(0, secondsLeft) / 5);
    return {
      hits: safeHits,
      total: total,
      accuracy: accuracy,
      xp: Math.round(6 * safeHits * accuracy) + speedBonus,
      shattered: safeHits === total && total > 0,
      shields: safeHits === total ? 1 : 0
    };
  }

  /* ------------------------------------------------- 7–12, 24. Seeds */
  function seedsForRoots(roots) {
    if (!roots) return [];
    const wanted = roots instanceof Set ? roots : new Set(roots);
    return seeds.filter((seed) => seed.root && wanted.has(seed.root));
  }
  function seedsForState(state) {
    const learned = state && state.items ? state.items : {};
    const roots = new Set();
    const catalogue = BAYAN.E && BAYAN.E.ITEMS ? BAYAN.E.ITEMS : {};
    for (const id in learned) {
      if (!learned[id] || learned[id].lvl < 1) continue;
      const item = catalogue[id];
      if (item && item.root) roots.add(item.root);
    }
    return seedsForRoots(roots);
  }
  function seedGroups() {
    const groups = {};
    for (const seed of seeds) {
      groups[seed.kind] = groups[seed.kind] || [];
      groups[seed.kind].push(seed);
    }
    return groups;
  }
  function periodicIndex(list, now, salt) {
    if (!list.length) return -1;
    const day = Math.floor(((Number.isFinite(now) ? now : 0) + (salt || 0)) / DAY);
    return ((day % list.length) + list.length) % list.length;
  }
  function whisperFor(now, salt) {
    const index = periodicIndex(whispers, now, salt || 0);
    return index < 0 ? null : whispers[index];
  }
  function loaderFor(id) {
    return loaders.find((entry) => entry.id === id) || null;
  }

  /* --------------------------------------------------------- 23. Sky */
  function skyForDate(date) {
    const hour = (date instanceof Date ? date : new Date(date || Date.now())).getHours();
    if (hour >= 4 && hour < 7) return "dawn";
    if (hour >= 7 && hour < 12) return "morning";
    if (hour >= 12 && hour < 15) return "noon";
    if (hour >= 15 && hour < 18) return "afternoon";
    if (hour >= 18 && hour < 21) return "dusk";
    return "night";
  }
  function skyClass(date) {
    return "sky-" + skyForDate(date);
  }

  /* ------------------------------------------------------ 13–18. Rites */
  function salahSteps() {
    return (rites.salah && rites.salah.steps) || [];
  }
  function waqtWindow(date) {
    const windows = (rites.waqt && rites.waqt.windows) || [];
    if (!windows.length) return null;
    const hour = (date instanceof Date ? date : new Date(date || Date.now())).getHours();
    const slot = WAQT_HOURS.find((entry) => hour < entry.upTo) || WAQT_HOURS[WAQT_HOURS.length - 1];
    return windows.find((entry) => entry.id === slot.id) || windows[0];
  }
  function jumuahOpen(date) {
    return (date instanceof Date ? date : new Date(date || Date.now())).getDay() === 5;
  }
  function wuduAdhanItems() {
    return (rites.wuduAdhan && rites.wuduAdhan.items) || [];
  }
  function zakatHajjItems() {
    return (rites.zakatHajj && rites.zakatHajj.items) || [];
  }
  function sawmDrills() {
    return (rites.sawm && rites.sawm.drills) || [];
  }
  function jumuahPhrases() {
    return (rites.jumuah && rites.jumuah.phrases) || [];
  }

  /* --------------------------------------------- 6. Illumination map */
  function items() {
    return (BAYAN.E && BAYAN.E.ITEMS) || {};
  }
  function knownRoots(state) {
    const catalogue = items();
    const roots = new Set();
    const learned = (state && state.items) || {};
    for (const id in learned) {
      if (!learned[id] || learned[id].lvl < 1) continue;
      const item = catalogue[id];
      if (item && item.root) roots.add(item.root);
    }
    return roots;
  }
  /* True when the three root letters appear in order inside the word — the same
   * loose check the map's illumination counter uses. */
  function carriesRoot(root, word) {
    if (!root || !word) return false;
    const letters = normalize(root).replace(/[^\u0621-\u064A]/g, "");
    const target = normalize(word);
    let cursor = 0;
    for (let i = 0; i < target.length && cursor < letters.length; i++) {
      if (target[i] === letters[cursor]) cursor += 1;
    }
    return cursor === letters.length;
  }
  function illumination(state) {
    const units = CUR.units || [];
    const roots = knownRoots(state);
    const verses = [];
    for (const unit of units) {
      for (const lesson of unit.lessons || []) {
        for (const verse of lesson.verses || []) {
          let lit = 0;
          const tokens = verse.tokens || [];
          for (const token of tokens) {
            let hit = false;
            roots.forEach((root) => { if (!hit && carriesRoot(root, token)) hit = true; });
            if (hit) lit += 1;
          }
          verses.push({
            id: verse.id, ref: verse.ref, ar: verse.ar, unit: unit.id, lesson: lesson.id,
            tokens: tokens.length, litTokens: lit, lit: lit > 0,
            fraction: tokens.length ? lit / tokens.length : 0
          });
        }
      }
    }
    const litVerses = verses.filter((v) => v.lit).length;
    return {
      roots: [...roots].sort(),
      rootCount: roots.size,
      verses: verses,
      totalVerses: verses.length,
      litVerses: litVerses,
      percent: verses.length ? Math.round((100 * litVerses) / verses.length) : 0
    };
  }

  /* -------------------------------------------------- 5. Ḥalaqah circle */
  function newCircle(maxMembers) {
    const cap = Number.isFinite(maxMembers) ? maxMembers : (F.halaqah && F.halaqah.maxMembers) || 5;
    return { cap: cap, members: [], lantern: 0, lastHelped: null };
  }
  function circleAdd(circle, name) {
    const trimmed = String(name == null ? "" : name).trim().slice(0, 24);
    if (!trimmed || !circle || circle.members.length >= circle.cap) return circle;
    if (circle.members.indexOf(trimmed) >= 0) return circle;
    circle.members.push(trimmed);
    return circle;
  }
  function circleTick(circle, practicedToday) {
    if (!circle) return circle;
    const active = Math.min(1, 1 / Math.max(2, circle.members.length + 1));
    circle.lantern = Math.max(0, Math.min(1, circle.lantern + (practicedToday ? active : -active / 2)));
    return circle;
  }
  function duaShield(circle, state) {
    if (!circle || circle.lantern >= 1) return false;
    circle.lantern = Math.min(1, circle.lantern + 0.25);
    circle.lastHelped = Date.now();
    if (state) state.shields = (state.shields || 0) + 1;
    return true;
  }

  BAYAN.F = {
    available: !!(F.combo && F.chests),
    raw: F,
    normalize: normalize, carriesRoot: carriesRoot,
    flowMultiplier: flowMultiplier, flowSemitones: flowSemitones, flowXp: flowXp, comboTick: comboTick,
    rollChest: rollChest, applyLoot: applyLoot, shieldAvailable: shieldAvailable, resolveStreakGap: resolveStreakGap,
    forgeRound: forgeRound,
    bossQueue: bossQueue, bossScore: bossScore, bossSeconds: () => boss.seconds || 45,
    seedsForRoots: seedsForRoots, seedsForState: seedsForState, seedGroups: seedGroups,
    whisperFor: whisperFor, loaderFor: loaderFor, loaders: loaders,
    skyForDate: skyForDate, skyClass: skyClass,
    salahSteps: salahSteps, waqtWindow: waqtWindow, jumuahOpen: jumuahOpen, jumuahPhrases: jumuahPhrases,
    wuduAdhanItems: wuduAdhanItems, zakatHajjItems: zakatHajjItems, sawmDrills: sawmDrills,
    illumination: illumination, knownRoots: knownRoots,
    newCircle: newCircle, circleAdd: circleAdd, circleTick: circleTick, duaShield: duaShield
  };
  window.Bayan = BAYAN;
})();
