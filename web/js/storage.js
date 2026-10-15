/* Portable v1 progress repository. No accounts, network, or async dependency.
   load() never repairs disk in place; import() only previews a validated clone.
   save() returns a boolean; import/export/normalize throw validation errors.
   status and error are UI-safe strings (error is null on success). */
(function () {
  "use strict";
  const KEY = "bayan.v1";
  const BACKUP = KEY + ".backup";
  const OLDER = BACKUP + ".2";
  const CORRUPT = KEY + ".corrupt";
  const MAX_BYTES = 2 * 1024 * 1024;
  const BLOCKED = new Set(["__proto__", "prototype", "constructor"]);
  const own = (object, key) => Object.prototype.hasOwnProperty.call(object, key);
  const Store = { status: "idle", error: null, source: null, MAX_BYTES,
    keys: Object.freeze({ primary: KEY, backup: BACKUP, older: OLDER, corrupt: CORRUPT }) };

  function fresh() {
    return {
      v: 1, onboarded: false, lang: "en", sound: true,
      xp: 0, gems: 0, streak: 0, day: null, dailyXp: 0, dailyGoal: 30,
      lessons: {}, unitsDone: {}, items: {}, achievements: {},
      stats: { answered: 0, correct: 0, lessons: 0, reviews: 0, perfects: 0, maxComboEver: 0, timeSpentSec: 0 }
    };
  }
  function fail(message) { throw new Error(message); }
  function record(value, label) {
    if (!value || Object.prototype.toString.call(value) !== "[object Object]") fail(label + " must be an object.");
    return value;
  }
  function number(value, label, max) {
    if (!Number.isSafeInteger(value) || value < 0 || value > (max === undefined ? Number.MAX_SAFE_INTEGER : max)) {
      fail(label + " must be a finite non-negative integer" + (max === undefined ? "." : " no greater than " + max + "."));
    }
    return value;
  }
  function boolean(value, label) {
    if (typeof value !== "boolean") fail(label + " must be true or false.");
    return value;
  }
  // Inspect even ignored fields: malicious keys must never be silently accepted.
  function inspect(value, seen, depth) {
    if (depth > 64) fail("Progress is nested too deeply.");
    if (value === null || typeof value === "string" || typeof value === "boolean") return;
    if (typeof value === "number") { if (!Number.isFinite(value)) fail("Progress contains a nonfinite number."); return; }
    if (typeof value !== "object") fail("Progress must contain JSON values only.");
    if (seen.has(value)) fail("Progress contains a cycle.");
    const proto = Object.getPrototypeOf(value);
    if (!Array.isArray(value) && proto && Object.getPrototypeOf(proto) !== null) fail("Progress contains a custom prototype.");
    if (!Array.isArray(value)) record(value, "Progress record");
    seen.add(value);
    for (const key of Object.keys(value)) {
      if (BLOCKED.has(key)) fail("Progress contains a forbidden prototype key.");
      const descriptor = Object.getOwnPropertyDescriptor(value, key);
      if (!descriptor || !own(descriptor, "value")) fail("Progress contains an accessor.");
      inspect(descriptor.value, seen, depth + 1);
    }
    seen.delete(value);
  }
  function checkSize(text) {
    if (typeof text !== "string") fail("Progress must be JSON text.");
    if (text.length > MAX_BYTES) fail("Progress exceeds the 2 MB limit.");
    // UTF-8 byte count without TextEncoder: works in older WebViews and Node VM.
    let bytes = 0;
    for (let i = 0; i < text.length; i++) {
      const code = text.charCodeAt(i);
      if (code < 0x80) bytes++;
      else if (code < 0x800) bytes += 2;
      else if (code >= 0xD800 && code <= 0xDBFF && i + 1 < text.length && text.charCodeAt(i + 1) >= 0xDC00 && text.charCodeAt(i + 1) <= 0xDFFF) { bytes += 4; i++; }
      else bytes += 3;
      if (bytes > MAX_BYTES) fail("Progress exceeds the 2 MB limit.");
    }
    return text;
  }
  function catalog() {
    const ids = { items: new Set(), lessons: new Set(), unitsDone: new Set() };
    const curriculum = window.BAYAN_CURRICULUM;
    if (!curriculum || !Array.isArray(curriculum.units)) return ids;
    for (const unit of curriculum.units) {
      ids.unitsDone.add(unit.id);
      for (const lesson of unit.lessons || []) {
        ids.lessons.add(lesson.id);
        for (const item of lesson.items || []) if (item.id) ids.items.add(item.id);
      }
    }
    return ids;
  }
  function date(value) {
    if (value === null) return null;
    if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) fail("day must be a calendar date or null.");
    const parsed = new Date(value + "T00:00:00.000Z");
    if (!Number.isFinite(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== value) fail("day is not a valid calendar date.");
    return value;
  }
  function fields(raw, base, label, limits) {
    record(raw, label);
    const out = Object.assign({}, base);
    for (const key of Object.keys(base)) {
      if (!own(raw, key)) continue;
      out[key] = typeof base[key] === "boolean" ? boolean(raw[key], label + "." + key) : number(raw[key], label + "." + key, limits && limits[key]);
    }
    return out;
  }
  function mergeState(raw, base) {
    record(raw, "State");
    if (own(raw, "v") && raw.v !== 1) fail("Unsupported progress version; expected v1.");
    const out = JSON.parse(JSON.stringify(base));
    for (const key of ["onboarded", "sound"]) if (own(raw, key)) out[key] = boolean(raw[key], key);
    for (const key of ["xp", "gems", "streak", "dailyXp", "dailyGoal"]) if (own(raw, key)) out[key] = number(raw[key], key);
    if (out.dailyGoal === 0) fail("dailyGoal must be greater than zero.");
    if (own(raw, "lang")) {
      if (raw.lang !== "en" && raw.lang !== "bn") fail("Unsupported progress language.");
      out.lang = raw.lang;
    }
    if (own(raw, "day")) out.day = date(raw.day);
    if (own(raw, "stats")) out.stats = fields(raw.stats, out.stats, "stats");
    const known = catalog();
    for (const key of ["items", "lessons"]) {
      if (!own(raw, key)) continue;
      record(raw[key], key);
      for (const id of Object.keys(raw[key])) {
        if (!known[key].has(id)) continue;
        const defaults = key === "items" ? { lvl: 0, seen: 0, ok: 0, bad: 0, dueAt: 0, flag: false } : { done: false, attempts: 0, stars: 0, lastAt: 0 };
        const limits = key === "items" ? { lvl: 5 } : { stars: 3 };
        out[key][id] = fields(raw[key][id], Object.assign(defaults, out[key][id]), key + "." + id, limits);
      }
    }
    if (own(raw, "unitsDone")) {
      const units = raw.unitsDone;
      if (Array.isArray(units)) {
        for (const id of units) {
          if (typeof id !== "string") fail("unitsDone must contain unit IDs.");
          if (known.unitsDone.has(id)) out.unitsDone[id] = true;
        }
      } else {
        record(units, "unitsDone");
        for (const id of Object.keys(units)) if (known.unitsDone.has(id)) out.unitsDone[id] = boolean(units[id], "unitsDone." + id);
      }
    }
    if (own(raw, "achievements")) {
      record(raw.achievements, "achievements");
      for (const id of Object.keys(raw.achievements)) {
        if (!/^[a-z][a-z0-9_]{0,79}$/.test(id)) fail("Invalid achievement ID.");
        out.achievements[id] = number(raw.achievements[id], "achievements." + id);
      }
    }
    return out;
  }
  function normalize(raw, defaults) {
    const value = raw === undefined ? {} : raw;
    inspect(value, new Set(), 0);
    checkSize(JSON.stringify(value));
    let base = fresh();
    if (defaults !== undefined) {
      inspect(defaults, new Set(), 0); checkSize(JSON.stringify(defaults));
      base = mergeState(defaults, base);
    }
    return mergeState(value, base);
  }
  function decode(text, strict, defaults) {
    checkSize(text);
    let raw;
    try { raw = JSON.parse(text); } catch (error) { fail("Progress is not valid JSON."); }
    inspect(raw, new Set(), 0); record(raw, "Progress");
    if (own(raw, "app") || own(raw, "version") || own(raw, "state")) {
      if (raw.app !== "bayan") fail("This backup is not for Bayan.");
      if (raw.version !== 1) fail("Unsupported backup version; expected version 1.");
      if (!own(raw, "state")) fail("Backup is missing its state.");
      raw = raw.state;
    } else if (strict && raw.v !== 1) fail("Raw progress must declare v: 1.");
    return normalize(raw, defaults);
  }
  function report(status, error, source) {
    Store.status = status; Store.error = error || null; Store.source = source || null;
    try {
      if (typeof window.dispatchEvent === "function" && typeof window.CustomEvent === "function") {
        window.dispatchEvent(new window.CustomEvent("bayan:storage", { detail: { status, error: Store.error, source: Store.source } }));
      }
    } catch (ignored) { /* Observer failures must not break persistence. */ }
  }
  function message(error) { return error && error.message ? error.message : String(error); }
  function storage() {
    const value = window.localStorage;
    if (!value) fail("Local storage is unavailable; export your progress to keep a backup.");
    return value;
  }
  function load(defaults) {
    let base;
    try { base = normalize({}, defaults); } catch (error) { report("error", message(error)); return fresh(); }
    const errors = [];
    try {
      const disk = storage();
      for (const key of [KEY, BACKUP, OLDER]) {
        const text = disk.getItem(key);
        if (text === null) continue;
        try {
          const state = decode(text, false, base);
          report(key === KEY ? "loaded" : "recovered", key === KEY ? null : "Recovered progress from a backup. " + errors.join(" "), key);
          return state;
        } catch (error) { errors.push(key + ": " + message(error)); }
      }
      report(errors.length ? "error" : "fresh", errors.length ? "Saved progress could not be read. " + errors.join(" ") : null);
    } catch (error) { report("error", "Unable to read local progress. " + message(error)); }
    return base;
  }
  function write(disk, key, text) {
    disk.setItem(key, text);
    if (disk.getItem(key) !== text) fail("Local storage did not retain " + key + ".");
  }
  function valid(text) {
    if (text === null) return false;
    try { decode(text, false); return true; } catch (ignored) { return false; }
  }
  function save(state) {
    try {
      const text = checkSize(JSON.stringify(normalize(state)));
      const disk = storage(), current = disk.getItem(KEY);
      if (current !== text && current !== null) {
        if (valid(current)) {
          const backup = disk.getItem(BACKUP);
          if (valid(backup)) write(disk, OLDER, backup);
          write(disk, BACKUP, current);
        } else {
          // Never rotate corrupt bytes over a usable backup. Preserve evidence
          // before a later explicit save replaces an unreadable primary.
          write(disk, CORRUPT, current);
        }
      }
      if (current !== text) write(disk, KEY, text);
      report("saved", null, KEY); return true;
    } catch (error) { report("error", "Progress was not saved. " + message(error)); return false; }
  }
  function clear() {
    try {
      const disk = storage();
      // Primary is last: a failure clearing recovery snapshots must not leave a
      // successful-looking reset that resurrects old progress on next launch.
      for (const key of [OLDER, BACKUP, CORRUPT, KEY]) {
        disk.removeItem(key);
        if (disk.getItem(key) !== null) fail("Local storage did not remove " + key + ".");
      }
      report("cleared"); return true;
    } catch (error) { report("error", "Progress was not reset. " + message(error)); return false; }
  }
  function exportProgress(state) {
    try {
      const text = checkSize(JSON.stringify({ app: "bayan", version: 1, state: normalize(state) }, null, 2));
      report("exported"); return text;
    } catch (error) { report("error", message(error)); throw error; }
  }
  function importProgress(text) {
    try { const state = decode(text, true); report("imported"); return state; }
    catch (error) { report("error", message(error)); throw error; }
  }
  Object.assign(Store, { load, save, clear, normalize, export: exportProgress, import: importProgress });
  window.Bayan = window.Bayan || {};
  window.Bayan.Store = Store;
})();
