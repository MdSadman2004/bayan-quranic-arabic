const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
const KEY = 'bayan.v1';
const BACKUP = KEY + '.backup';
const OLDER = BACKUP + '.2';
function fixture(initial = {}, options = {}) {
  const disk = new Map(Object.entries(initial));
  const writes = [];
  const storage = {
    getItem(key) { if (options.readError) throw new Error('Storage disabled'); return disk.has(key) ? disk.get(key) : null; },
    setItem(key, text) {
      if (options.quota || options.failKey === key) throw new Error('QuotaExceededError');
      writes.push(key); if (!options.dropWrites) disk.set(key, text);
    },
    removeItem(key) { disk.delete(key); }
  };
  const window = { Bayan: { marker: 7 } };
  if (options.getterError) Object.defineProperty(window, 'localStorage', { get() { throw new Error('SecurityError'); } });
  else window.localStorage = storage;
  const context = vm.createContext({ window });
  vm.runInContext(fs.readFileSync(path.join(root, 'web/js/data.js'), 'utf8'), context);
  vm.runInContext(fs.readFileSync(path.join(root, 'web/js/storage.js'), 'utf8'), context);
  return { Store: window.Bayan.Store, window, disk, writes, options };
}
const plain = value => JSON.parse(JSON.stringify(value));
function populated(Store) {
  const state = Store.load();
  Object.assign(state, { onboarded: true, lang: 'bn', xp: 42, gems: 3, day: '2026-10-15', streak: 2 });
  state.items.w_hadha = { lvl: 2, seen: 3, ok: 4, bad: 1, dueAt: 1792036800000, flag: false };
  state.lessons.u1l1 = { done: true, attempts: 2, stars: 3, lastAt: 1792036800000, placed: false };
  state.unitsDone.u1 = true;
  state.achievements.first_lesson = 1792036800000;
  state.stats.answered = 12; state.stats.correct = 10;
  return state;
}
test('attaches to the existing Bayan namespace and defaults are independent clones', () => {
  const { Store, window } = fixture(); const a = Store.load(), b = Store.load();
  assert.equal(window.Bayan.marker, 7); assert.equal(a.v, 1); assert.equal(a.dailyGoal, 30);
  assert.equal(Store.status, 'fresh'); assert.equal(Store.error, null);
  a.stats.correct = 10; assert.equal(b.stats.correct, 0);
});
test('partial state deeply merges required nested defaults without modifying its input', () => {
  const { Store } = fixture(); const raw = { v: 1, stats: { answered: 5 }, items: { w_hadha: { seen: 1 } }, lessons: { u1l1: { done: true } } };
  const before = JSON.stringify(raw), state = Store.normalize(raw);
  assert.equal(JSON.stringify(raw), before);
  assert.deepEqual(plain(state.stats), { answered: 5, correct: 0, lessons: 0, reviews: 0, perfects: 0, maxComboEver: 0, timeSpentSec: 0 });
  assert.deepEqual(plain(state.items.w_hadha), { lvl: 0, seen: 1, ok: 0, bad: 0, dueAt: 0, flag: false });
  assert.deepEqual(plain(state.lessons.u1l1), { done: true, attempts: 0, stars: 0, lastAt: 0, placed: false });
});
test('load accepts optional defaults and deeply merges partial disk state', () => {
  const { Store } = fixture({ [KEY]: JSON.stringify({ v: 1, stats: { answered: 8 } }) });
  const state = Store.load({ v: 1, lang: 'bn', dailyGoal: 50, stats: { reviews: 3 } });
  assert.equal(state.lang, 'bn'); assert.equal(state.dailyGoal, 50); assert.equal(state.stats.reviews, 3); assert.equal(state.stats.answered, 8);
});
test('save reads back its write and load preserves portable progress', () => {
  const { Store, disk } = fixture(); const state = populated(Store);
  assert.equal(Store.save(state), true); assert.equal(Store.status, 'saved'); assert.equal(Store.error, null);
  assert.deepEqual(plain(Store.load()), plain(state)); assert.equal(JSON.parse(disk.get(KEY)).v, 1);
});
test('exports a v1 Bayan envelope and import returns a validated clone without writing', () => {
  const { Store, disk, writes } = fixture(); const state = populated(Store);
  const text = Store.export(state), envelope = JSON.parse(text), imported = Store.import(text);
  assert.equal(envelope.app, 'bayan'); assert.equal(envelope.version, 1);
  assert.deepEqual(plain(imported), plain(state)); imported.stats.answered = 99;
  assert.equal(state.stats.answered, 12); assert.equal(disk.size, 0); assert.equal(writes.length, 0);
});
test('imports raw Android v1 and normalizes the legacy unitsDone array', () => {
  const { Store } = fixture(); const state = Store.import(JSON.stringify({ v: 1, unitsDone: ['u1', 'ghost'] }));
  assert.deepEqual(plain(state.unitsDone), { u1: true });
});
test('ignores unknown vocabulary, lessons and units rather than entering review queues', () => {
  const { Store } = fixture(); const state = Store.normalize({ v: 1, items: { ghost: { seen: 2 }, w_hadha: {} }, lessons: { ghost: {}, u1l1: {} }, unitsDone: { ghost: true, u1: true } });
  assert.deepEqual(Object.keys(state.items), ['w_hadha']); assert.deepEqual(Object.keys(state.lessons), ['u1l1']);
  assert.deepEqual(Object.keys(state.unitsDone), ['u1']);
});
test('rotates the last two valid snapshots before replacing primary', () => {
  const { Store, disk } = fixture();
  for (const xp of [10, 20, 30]) { const state = Store.load(); state.xp = xp; assert.equal(Store.save(state), true); }
  assert.equal(JSON.parse(disk.get(KEY)).xp, 30); assert.equal(JSON.parse(disk.get(BACKUP)).xp, 20); assert.equal(JSON.parse(disk.get(OLDER)).xp, 10);
});
test('corrupt primary recovers backup without overwriting any on-disk evidence', () => {
  const original = { [KEY]: '{bad', [BACKUP]: JSON.stringify({ v: 1, xp: 99 }) };
  const { Store, disk, writes } = fixture(original); assert.equal(Store.load().xp, 99);
  assert.equal(Store.status, 'recovered'); assert.ok(Store.error); assert.equal(disk.get(KEY), original[KEY]); assert.equal(writes.length, 0);
});
test('falls back past a malformed first backup and reports all-corrupt failure', () => {
  const f = fixture({ [KEY]: '{}broken', [BACKUP]: '[]', [OLDER]: '{"v":1,"xp":17}' });
  assert.equal(f.Store.load().xp, 17); assert.equal(f.Store.source, OLDER);
  const bad = fixture({ [KEY]: 'no', [BACKUP]: 'no', [OLDER]: 'null' });
  assert.equal(bad.Store.load().xp, 0); assert.equal(bad.Store.status, 'error'); assert.ok(bad.Store.error); assert.equal(bad.writes.length, 0);
});
test('a missing primary can recover an existing backup', () => {
  const { Store } = fixture({ [BACKUP]: '{"v":1,"xp":3}' });
  assert.equal(Store.load().xp, 3); assert.equal(Store.status, 'recovered');
});
test('explicit save after recovery preserves corrupt text and valid recovery snapshots', () => {
  const { Store, disk } = fixture({ [KEY]: '{bad', [BACKUP]: '{"v":1,"xp":9}' });
  const state = Store.load(); state.xp = 10; assert.equal(Store.save(state), true);
  assert.equal(disk.get(KEY + '.corrupt'), '{bad'); assert.equal(JSON.parse(disk.get(BACKUP)).xp, 9); assert.equal(Store.load().xp, 10);
});
test('storage access exceptions and throwing localStorage getters never escape load/save', () => {
  for (const options of [{ readError: true }, { getterError: true }]) {
    const { Store } = fixture({}, options); assert.equal(Store.load().v, 1); assert.equal(Store.status, 'error');
    assert.equal(Store.save(Store.normalize({})), false); assert.ok(Store.error);
  }
});
test('quota during backup rotation is non-destructive to the existing primary', () => {
  const before = '{"v":1,"xp":8}'; const { Store, disk } = fixture({ [KEY]: before }, { failKey: BACKUP });
  const state = Store.load(); state.xp = 9; assert.equal(Store.save(state), false); assert.equal(disk.get(KEY), before); assert.ok(Store.error);
});
test('quota on first save and silent dropped writes report failure', () => {
  for (const options of [{ quota: true }, { dropWrites: true }]) {
    const { Store } = fixture({}, options); assert.equal(Store.save(Store.load()), false); assert.equal(Store.status, 'error'); assert.ok(Store.error);
  }
});
const invalidStates = [
  { v: 2 }, { v: 1, xp: -1 }, { v: 1, stats: { answered: -1 } }, { v: 1, stats: [] },
  { v: 1, items: { w_hadha: 'broken' } }, { v: 1, lessons: { u1l1: null } },
  { v: 1, items: { w_hadha: { lvl: 6 } } }, { v: 1, lessons: { u1l1: { stars: 4 } } },
  { v: 1, lang: 'xx' }, { v: 1, day: '2026-02-30' }, { v: 1, onboarded: 'yes' },
  { v: 1, unitsDone: { u1: 'true' } }, { v: 1, achievements: { first_lesson: -1 } }
];
test('malformed records and unsupported state versions are rejected without disk mutation', () => {
  const { Store, disk, writes } = fixture({ [KEY]: '{"v":1,"xp":7}' });
  for (const state of invalidStates) assert.throws(() => Store.import(JSON.stringify({ app: 'bayan', version: 1, state })), undefined, JSON.stringify(state));
  assert.equal(disk.get(KEY), '{"v":1,"xp":7}'); assert.equal(writes.length, 0);
});
test('rejects malformed envelopes, missing raw version, invalid JSON and oversized UTF-8 input', () => {
  const { Store } = fixture();
  for (const text of ['{', 'null', '[]', '{}', '{"app":"other","version":1,"state":{"v":1}}', '{"app":"bayan","version":2,"state":{"v":1}}', '{"app":"bayan","version":1}', '"' + 'a'.repeat(Store.MAX_BYTES + 1) + '"', '{"v":1,"extra":"' + 'ব'.repeat(750000) + '"}']) assert.throws(() => Store.import(text));
});
test('prototype pollution keys at every level, cycles and nonfinite numbers are rejected', () => {
  const { Store } = fixture();
  for (const text of ['{"v":1,"__proto__":{}}', '{"v":1,"stats":{"constructor":{}}}', '{"v":1,"items":{"prototype":{}}}', '{"v":1,"xp":1e999}']) assert.throws(() => Store.import(text));
  for (const number of [NaN, Infinity, -Infinity]) assert.throws(() => Store.normalize({ v: 1, stats: { answered: number } }));
  const cyclic = { v: 1 }; cyclic.self = cyclic; assert.throws(() => Store.normalize(cyclic));
  assert.equal({}.polluted, undefined);
});
test('invalid save/export do not erase already saved data', () => {
  const { Store, disk } = fixture({ [KEY]: '{"v":1,"xp":5}' });
  assert.equal(Store.save({ v: 1, xp: NaN }), false); assert.throws(() => Store.export({ v: 2 }));
  assert.equal(disk.get(KEY), '{"v":1,"xp":5}');
});
