/* Bayan engine — curriculum index, SRS, gamification, lesson compiler.
   Pure logic, no DOM. Mirrored 1:1 by the Android client's Engine.kt. */
(function () {
  "use strict";
  const CUR = window.BAYAN_CURRICULUM || { units: [], totalQuranWords: 77429 };
  const DAY = 86400000;
  const INTERVALS = [0, 1, 2, 4, 8, 16]; // SRS level 0..5 -> days until due
  const MAX_HEARTS = 5;
  const REVIEW_SIZE = 12;

  /* ------------------------------------------------- flags */
  if (typeof Object.freeze === "function") Object.freeze(INTERVALS);

  /* ------------------------------------------------- utility */
  function hash(str) {
    let h = 1779033703 ^ str.length;
    for (let i = 0; i < str.length; i++) {
      h = Math.imul(h ^ str.charCodeAt(i), 3432918353);
      h = (h << 13) | (h >>> 19);
    }
    return h >>> 0;
  }
  function rng(seed) {
    let a = hash(String(seed));
    return function () {
      a |= 0; a = (a + 0x6D2B79F5) | 0;
      let t = Math.imul(a ^ (a >>> 15), 1 | a);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }
  function shuffle(arr, rand) {
    const a = arr.slice();
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(rand() * (i + 1));
      const t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }

  /* ------------------------------------------------- index */
  const ITEMS = {}, ITEMS_ORDER = [], UNITS = [], LESSONS = {};
  const TOKENS = [];
  for (const unit of CUR.units) {
    const u = { id: unit.id, num: unit.num, title: unit.title, subtitle: unit.subtitle, lessons: [] };
    for (const lesson of unit.lessons) {
      const resolved = lesson.items.map(function (it) {
        if (it.ref) return ITEMS[it.ref];
        ITEMS[it.id] = it; ITEMS_ORDER.push(it.id); return it;
      });
      const l = Object.assign({}, lesson, { unit: unit.id, resolvedItems: resolved.map(x => x) });
      LESSONS[lesson.id] = l;
      u.lessons.push(lesson.id);
      for (const v of lesson.verses) for (const tk of v.tokens) if (TOKENS.indexOf(tk) < 0) TOKENS.push(tk);
    }
    UNITS.push(u);
  }
  const ALL_UNIT_IDS = UNITS.map(u => u.id);
  const allItems = () => ITEMS_ORDER.map(id => ITEMS[id]);
  const itemObject = id => ITEMS[id];
  const lessonObject = id => LESSONS[id];
  const unitObject = id => UNITS.find(u => u.id === id);
  const unitOfLesson = id => (LESSONS[id] ? LESSONS[id].unit : null);
  function unitItems(unitId) {
    const u = unitObject(unitId), out = [];
    for (const lid of u.lessons) for (const it of LESSONS[lid].resolvedItems)
      if (!out.some(o => o.id === it.id)) out.push(it);
    return out;
  }
  const meaning = (item, lang) => (lang === "bn" ? item.bn : item.en);

  /* ------------------------------------------------- state */
  function newState() {
    return {
      v: 1, onboarded: false, lang: "en", sound: true,
      xp: 0, gems: 0, streak: 0, day: null, dailyXp: 0, dailyGoal: 30,
      lessons: {}, unitsDone: {}, items: {}, achievements: {},
      stats: { answered: 0, correct: 0, lessons: 0, reviews: 0, perfects: 0, maxComboEver: 0, timeSpentSec: 0 }
    };
  }
  function load() {
    return window.Bayan.Store.load();
  }
  function save(s) {
    return window.Bayan.Store.save(s);
  }
  function dayKey(ts) {
    const d = new Date(ts);
    return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
  }

  /* ------------------------------------------------- progress */
  const lessonDone = (s, id) => !!(s.lessons[id] && s.lessons[id].done);
  function isUnlocked(s, lessonId) {
    if (!LESSONS[lessonId]) return false;
    const uid = unitOfLesson(lessonId), u = unitObject(uid), i = u.lessons.indexOf(lessonId);
    if (i === 0) {
      const ui = ALL_UNIT_IDS.indexOf(uid);
      if (ui === 0) return true;
      const prev = unitObject(ALL_UNIT_IDS[ui - 1]);
      return prev.lessons.every(l => lessonDone(s, l));
    }
    return lessonDone(s, u.lessons[i - 1]);
  }
  function nextLessonId(s) {
    for (const u of UNITS) for (const lid of u.lessons) if (!lessonDone(s, lid)) return lid;
    return null;
  }
  function unitProgress(s, unitId) {
    const u = unitObject(unitId);
    let n = 0;
    for (const lid of u.lessons) if (lessonDone(s, lid)) n++;
    return { done: n, total: u.lessons.length };
  }
  function doneLessons(s) {
    let n = 0;
    for (const lid in LESSONS) if (lessonDone(s, lid)) n++;
    return n;
  }

  /* ------------------------------------------------- SRS */
  const isSeen = (s, id) => !!(s.items[id] && s.items[id].seen > 0);
  function seenCount(s) {
    let n = 0;
    for (const id of ITEMS_ORDER) if (s.items[id] && s.items[id].seen > 0) n++;
    return n;
  }
  function learnedCount(s) {
    let n = 0;
    for (const id of ITEMS_ORDER) if (s.items[id] && s.items[id].lvl >= 1) n++;
    return n;
  }
  function masteredCount(s) {
    let n = 0;
    for (const id of ITEMS_ORDER) if (s.items[id] && s.items[id].lvl >= 3) n++;
    return n;
  }
  function sortByNeed(s, ids) {
    return ids.slice().sort((a, b) => {
      const ra = s.items[a], rb = s.items[b];
      if (ra.flag !== rb.flag) return ra.flag ? -1 : 1;
      if (ra.lvl !== rb.lvl) return ra.lvl - rb.lvl;
      return ra.dueAt - rb.dueAt;
    });
  }
  function dueIds(s, now) {
    now = now == null ? Date.now() : now;
    const out = [];
    for (const id in s.items) {
      const r = s.items[id];
      if (ITEMS[id] && r.seen > 0 && r.dueAt <= now) out.push(id);
    }
    return sortByNeed(s, out);
  }
  function reviewQueue(s, now) {
    now = now == null ? Date.now() : now;
    let q = dueIds(s, now).slice(0, REVIEW_SIZE);
    if (q.length < 6) {
      const seen = ITEMS_ORDER.filter(id => isSeen(s, id) && q.indexOf(id) < 0);
      q = q.concat(sortByNeed(s, seen).slice(0, 6 - q.length));
    }
    return q;
  }
  function coverage(s) {
    let occ = 0;
    for (const id of ITEMS_ORDER) {
      const r = s.items[id];
      if (r && r.lvl >= 1 && ITEMS[id].occ) occ += ITEMS[id].occ;
    }
    return (100 * occ) / (CUR.totalQuranWords || 77429);
  }

  /* ------------------------------------------------- compiler */
  function pickOptions(item, correctText, textOf, rand, pools) {
    const texts = new Set([correctText, item.ar, item.en, item.bn]);
    const out = [correctText];
    for (const pool of pools) {
      for (const c of shuffle(pool, rand)) {
        if (out.length >= 4) break;
        if (c.id === item.id) continue;
        const t = textOf(c);
        if (texts.has(t)) continue;
        texts.add(t); out.push(t);
      }
      if (out.length >= 4) break;
    }
    const sh = shuffle(out, rand);
    return { options: sh, answer: sh.indexOf(correctText) };
  }

  function makeTools(cards, lang, rand, pools, canListen) {
    const text = (c) => meaning(c, lang);
    const tools = {};
    tools.addMc = function (item) {
      if (!item) return;
      const o = pickOptions(item, text(item), text, rand, pools);
      cards.push({ t: "mc", id: item.id, ref: item, prompt: item.ar, options: o.options, answer: o.answer });
    };
    tools.addMca = function (item) {
      if (!item) return;
      const o = pickOptions(item, item.ar, c => c.ar, rand, pools);
      cards.push({ t: "mca", id: item.id, ref: item, prompt: text(item), options: o.options, answer: o.answer });
    };
    tools.addRoot = function (item) {
      if (!item || !item.root) return;
      const roots = [item.root];
      for (const c of shuffle(pools[1], rand)) {
        if (roots.length >= 4) break;
        if (c.root && roots.indexOf(c.root) < 0) roots.push(c.root);
      }
      const sh = shuffle(roots, rand);
      cards.push({ t: "root", id: item.id, ref: item, prompt: item.ar, options: sh, answer: sh.indexOf(item.root) });
    };
    tools.addMatch = function (items) {
      if (items.length < 3) return;
      const chosen = items.slice(0, 4);
      const pairs = chosen.map(x => ({ id: x.id, left: x.ar, right: text(x) }));
      cards.push({
        t: "match", id: chosen.map(x => x.id).join("+"), pairs: pairs,
        rights: shuffle(pairs.map(p => p.id), rand)
      });
    };
    tools.addListen = function (item) {
      if (!item || !canListen) return;
      const o = pickOptions(item, item.ar, c => c.ar, rand, pools);
      cards.push({ t: "listen", id: item.id, ref: item, prompt: "", options: o.options, answer: o.answer });
    };
    tools.addBuild = function (v) {
      const inVerse = new Set(v.tokens);
      const extras = [];
      for (const c of shuffle(TOKENS, rand)) {
        if (extras.length >= 2) break;
        if (!inVerse.has(c)) extras.push(c);
      }
      cards.push({ t: "build", id: v.id, verse: v, tiles: shuffle(v.tokens.concat(extras), rand), answer: v.ar });
    };
    tools.addBlank = function (v) {
      const idxs = [];
      v.tokens.forEach((tk, i) => {
        if (i >= 1 && tk.replace(/[^\u0600-\u06FF]/g, "").length >= 4) idxs.push(i);
      });
      const idx = idxs.length ? idxs[Math.floor(rand() * idxs.length)] : Math.max(1, v.tokens.length - 1);
      const correct = v.tokens[idx];
      const opts = [correct];
      for (const c of shuffle(TOKENS, rand)) {
        if (opts.length >= 4) break;
        if (c !== correct && v.tokens.indexOf(c) < 0) opts.push(c);
      }
      const sh = shuffle(opts, rand);
      cards.push({ t: "blank", id: v.id, verse: v, blank: idx, options: sh, answer: sh.indexOf(correct) });
    };
    tools.addRead = function (v) {
      if (v.context) cards.push({ t: "context", id: v.id, verse: v });
      cards.push({ t: "read", id: v.id, verse: v });
    };
    return tools;
  }

  function compileLesson(lessonId, s, opts) {
    opts = opts || {};
    const lesson = LESSONS[lessonId];
    if (!lesson) return [];
    const lang = s.lang;
    const rand = rng(lessonId + ":" + ((s.lessons[lessonId] && s.lessons[lessonId].attempts) || 0));
    const pools = [unitItems(lesson.unit), allItems()];
    const canListen = !!(opts.tts && opts.tts());
    const cards = [];
    const tools = makeTools(cards, lang, rand, pools, canListen);
    const it = lesson.resolvedItems;
    const vs = lesson.verses;
    const te = lesson.teaches;

    if (te[0]) cards.push({ t: "teach", id: te[0].id, teach: te[0] });
    tools.addMc(it[0]);
    tools.addMc(it[1]);
    tools.addMatch(it.slice(0, 4));
    if (te[1]) cards.push({ t: "teach", id: te[1].id, teach: te[1] });
    tools.addMca(it[2]);
    tools.addMc(it[3]);
    if (canListen) { tools.addListen(it[it.length - 1]); tools.addListen(it[2]); }
    it.filter(x => x.root).slice(0, 2).forEach(tools.addRoot);

    const v0 = vs[0];
    if (v0) { tools.addRead(v0); if (v0.tokens.length <= 12) tools.addBuild(v0); tools.addBlank(v0); }

    if (te[2]) cards.push({ t: "teach", id: te[2].id, teach: te[2] });
    it.slice(4).forEach((x, i) => (i % 2 === 0 ? tools.addMc(x) : tools.addMca(x)));

    const v1 = vs[1];
    if (v1) { tools.addRead(v1); if (v1.tokens.length <= 12) tools.addBuild(v1); tools.addBlank(v1); }

    cards.push({ t: "complete", id: lessonId });
    return cards;
  }

  function compileReview(s, ids, opts) {
    opts = opts || {};
    const rand = rng("review:" + dayKey(Date.now()) + ":" + ids.join(","));
    const pools = [allItems(), allItems()];
    const canListen = !!(opts.tts && opts.tts());
    const cards = [];
    const tools = makeTools(cards, s.lang, rand, pools, canListen);
    const pool = [...new Set(ids)].map(id => ITEMS[id]).filter(Boolean);
    tools.addMc(pool[0]);
    tools.addMc(pool[1]);
    tools.addMatch(pool.slice(0, 4));
    pool.forEach((item, i) => {
      if (i < 2) return;
      const k = i % 4;
      if (k === 2 && item.root) tools.addRoot(item);
      else if (k === 3 && canListen) tools.addListen(item);
      else if (k === 1) tools.addMca(item);
      else tools.addMc(item);
    });
    cards.push({ t: "complete", id: "review" });
    return cards;
  }

  function check(card, answer) {
    switch (card.t) {
      case "mc": case "mca": case "root": case "listen": case "blank":
        return answer === card.answer;
      case "build":
        return answer === card.answer;
      default:
        return true;
    }
  }

  /* ------------------------------------------------- results */
  function applyLessonResult(s, run) {
    const now = Date.now();
    if (run.mode !== "review" && !LESSONS[run.lessonId]) throw new Error("Unknown lesson result");
    const acc = run.total ? run.correct / run.total : 1;
    const perfect = run.total > 0 && run.mistakes === 0;
    const isReview = run.mode === "review";
    let xp = isReview ? 5 + Math.round(3 * acc) : 10 + Math.round(5 * acc);
    if (perfect) xp += 5;
    xp += Math.min(3, Math.floor(run.maxCombo / 5)) * 2;

    let unitComplete = null;
    if (!isReview && run.lessonId) {
      const lesson = LESSONS[run.lessonId];
      const rec = s.lessons[run.lessonId] = s.lessons[run.lessonId] || {};
      rec.done = true;
      rec.attempts = (rec.attempts || 0) + 1;
      rec.stars = Math.max(rec.stars || 0, acc === 1 ? 3 : acc >= 0.8 ? 2 : 1);
      rec.lastAt = now;
      const u = unitObject(lesson.unit);
      if (u.lessons.every(l => lessonDone(s, l)) && !s.unitsDone[u.id]) {
        s.unitsDone[u.id] = true;
        unitComplete = u;
        xp += 20;
      }
    }
    let gems = perfect ? 2 : 1;
    if (unitComplete) gems += 5;
    s.xp += xp;
    s.gems += gems;

    const dk = dayKey(now);
    if (s.day !== dk) {
      const yesterday = new Date(now);
      yesterday.setDate(yesterday.getDate() - 1);
      s.streak = s.day === dayKey(yesterday.getTime()) ? s.streak + 1 : 1;
      s.day = dk;
      s.dailyXp = 0;
    }
    s.dailyXp += xp;

    s.stats.lessons += isReview ? 0 : 1;
    s.stats.reviews += isReview ? 1 : 0;
    s.stats.answered += run.total;
    s.stats.correct += run.correct;
    if (perfect) s.stats.perfects += 1;
    s.stats.maxComboEver = Math.max(s.stats.maxComboEver, run.maxCombo || 0);
    s.stats.timeSpentSec += run.secs || 0;

    for (const id in run.touches) {
      // Verse-building grades count toward accuracy, not vocabulary mastery.
      if (!ITEMS[id]) continue;
      const tou = run.touches[id];
      const r = s.items[id] = s.items[id] || { lvl: 0, seen: 0, ok: 0, bad: 0, dueAt: 0, flag: false };
      const advance = r.seen === 0 || r.dueAt <= now;
      r.seen += 1;
      r.ok += tou.ok;
      r.bad += tou.bad;
      if (tou.bad > 0) {
        r.lvl = Math.max(0, r.lvl - 1);
        r.flag = true;
        r.dueAt = now;
      } else if (advance) {
        r.lvl = Math.min(5, r.lvl + 1);
        r.flag = false;
        r.dueAt = now + INTERVALS[r.lvl] * DAY;
      }
    }

    const newAch = evaluateAchievements(s);
    return { xp: xp, acc: acc, perfect: perfect, gems: gems, unitComplete: unitComplete, newAch: newAch };
  }

  function activeStreak(s, now) {
    now = now == null ? Date.now() : now;
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    return s.day === dayKey(now) || s.day === dayKey(yesterday.getTime()) ? s.streak : 0;
  }
  function mistakeIds(s) {
    return sortByNeed(s, ITEMS_ORDER.filter(id => isSeen(s, id) && s.items[id].flag));
  }
  function searchItems(s, query, filter) {
    const normalize = text => String(text).normalize("NFD").replace(/[\u064B-\u065F\u0670\u0640\s]/g, "").toLowerCase();
    const q = normalize(query || "");
    return allItems().filter(it => {
      const r = s.items[it.id];
      if (filter === "seen" && !isSeen(s, it.id)) return false;
      if (filter === "mistakes" && !(r && r.flag)) return false;
      return !q || [it.ar, it.en, it.bn, it.root || ""].some(v => normalize(v).includes(q));
    });
  }

  /* ------------------------------------------------- achievements */
  const ACHIEVEMENTS = [
    { id: "first_lesson", icon: "🌱", name: { en: "First Steps", bn: "প্রথম পদক্ষেপ" }, desc: { en: "Complete your first lesson", bn: "প্রথম পাঠ সম্পন্ন করুন" }, test: s => s.stats.lessons >= 1 },
    { id: "lessons_10", icon: "📚", name: { en: "Ten Lessons", bn: "দশটি পাঠ" }, desc: { en: "Complete 10 lessons", bn: "১০টি পাঠ শেষ করুন" }, test: s => s.stats.lessons >= 10 },
    { id: "streak_3", icon: "🔥", name: { en: "Warming Up", bn: "গতি এসেছে" }, desc: { en: "Reach a 3-day streak", bn: "৩ দিনের স্ট্রিক" }, test: s => s.streak >= 3 },
    { id: "streak_7", icon: "☀️", name: { en: "Consistent", bn: "নিয়মিত" }, desc: { en: "Reach a 7-day streak", bn: "৭ দিনের স্ট্রিক" }, test: s => s.streak >= 7 },
    { id: "xp_100", icon: "⚡", name: { en: "Century", bn: "শতক" }, desc: { en: "Earn 100 XP", bn: "১০০ XP অর্জন" }, test: s => s.xp >= 100 },
    { id: "xp_500", icon: "🌟", name: { en: "Five Hundred", bn: "পাঁচশ" }, desc: { en: "Earn 500 XP", bn: "৫০০ XP অর্জন" }, test: s => s.xp >= 500 },
    { id: "perfect", icon: "🎯", name: { en: "Flawless", bn: "নিখুঁত" }, desc: { en: "Finish a lesson with no mistakes", bn: "একটি পাঠ ভুল ছাড়া শেষ করুন" }, test: s => s.stats.perfects >= 1 },
    { id: "unit1", icon: "🏛️", name: { en: "Foundations Set", bn: "ভিত্তি স্থাপিত" }, desc: { en: "Complete Unit 1", bn: "ইউনিট ১ সম্পন্ন করুন" }, test: s => lessonDone(s, "u1l3") },
    { id: "words_25", icon: "📖", name: { en: "25 Words", bn: "২৫ শব্দ" }, desc: { en: "Learn 25 words", bn: "২৫টি শব্দ শিখুন" }, test: s => learnedCount(s) >= 25 },
    { id: "words_50", icon: "🕌", name: { en: "50 Words", bn: "৫০ শব্দ" }, desc: { en: "Learn 50 words", bn: "৫০টি শব্দ শিখুন" }, test: s => learnedCount(s) >= 50 },
    { id: "review_1", icon: "🔁", name: { en: "Reviewer", bn: "রিভিউয়ার" }, desc: { en: "Complete a review session", bn: "একটি রিভিউ সেশন শেষ করুন" }, test: s => s.stats.reviews >= 1 },
    { id: "halfway", icon: "🧭", name: { en: "Halfway", bn: "অর্ধেক পথ" }, desc: { en: "Complete 12 lessons", bn: "১২টি পাঠ শেষ করুন" }, test: s => doneLessons(s) >= 12 }
  ];
  function evaluateAchievements(s) {
    const got = [];
    for (const a of ACHIEVEMENTS) {
      if (!s.achievements[a.id] && a.test(s)) {
        s.achievements[a.id] = Date.now();
        got.push(a.id);
      }
    }
    return got;
  }

  /* ------------------------------------------------- export */
  window.Bayan = window.Bayan || {};
  window.Bayan.CUR = CUR;
  window.Bayan.E = {
    CUR, UNITS, LESSONS, ITEMS, ITEMS_ORDER, TOKENS, ALL_UNIT_IDS,
    allItems, itemObject, lessonObject, unitObject, unitOfLesson, unitItems, meaning,
    newState, load, save, dayKey,
    lessonDone, isUnlocked, nextLessonId, unitProgress, doneLessons,
    isSeen, seenCount, learnedCount, masteredCount, dueIds, reviewQueue, coverage, activeStreak, mistakeIds, searchItems,
    compileLesson, compileReview, check, applyLessonResult,
    ACHIEVEMENTS, evaluateAchievements,
    MAX_HEARTS, REVIEW_SIZE, INTERVALS, rng, shuffle
  };
})();
