const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const NOW = new Date(2026, 9, 15, 12).getTime();
const DAY = 86400000;
function engine() {
  class Clock extends Date { static now() { return NOW; } }
  const c = vm.createContext({ window: {}, Date: Clock, localStorage: { getItem: () => null, setItem() {} } });
  for (const file of ['data', 'engine']) vm.runInContext(fs.readFileSync(path.join(root, `web/js/${file}.js`), 'utf8'), c);
  return c.window.Bayan.E;
}
function result(E, id = E.ITEMS_ORDER[0], changes = {}) {
  return Object.assign({ mode: 'lesson', lessonId: 'u1l1', total: 4, correct: 4, mistakes: 0, maxCombo: 4, secs: 30, touches: { [id]: { ok: 2, bad: 0 } } }, changes);
}
function seen(lvl = 1, dueAt = NOW + DAY) { return { lvl, seen: 1, ok: 2, bad: 0, dueAt, flag: false }; }
test('a missing calendar day resets streak instead of incrementing it', () => {
  const E = engine(), s = E.newState();
  s.day = E.dayKey(NOW - 3 * DAY); s.streak = 7; s.dailyXp = 99;
  const r = E.applyLessonResult(s, result(E));
  assert.equal(s.streak, 1); assert.equal(s.dailyXp, r.xp);
});
test('consecutive day increments streak, second lesson today does not', () => {
  const E = engine(), s = E.newState(); s.day = E.dayKey(NOW - DAY); s.streak = 2;
  E.applyLessonResult(s, result(E)); assert.equal(s.streak, 3);
  E.applyLessonResult(s, result(E)); assert.equal(s.streak, 3);
});
test('early repetition cannot turn a new word into a mastered word today', () => {
  const E = engine(), s = E.newState(), id = E.ITEMS_ORDER[0];
  E.applyLessonResult(s, result(E, id));
  const due = s.items[id].dueAt;
  for (let i = 0; i < 5; i++) E.applyLessonResult(s, result(E, id));
  assert.equal(s.items[id].lvl, 1); assert.equal(s.items[id].dueAt, due);
});
test('a due word advances one SRS level', () => {
  const E = engine(), s = E.newState(), id = E.ITEMS_ORDER[0]; s.items[id] = seen(1, NOW);
  E.applyLessonResult(s, result(E, id));
  assert.equal(s.items[id].lvl, 2); assert.equal(s.items[id].dueAt, NOW + 2 * DAY);
});
test('wrong word is due immediately and records correct touches too', () => {
  const E = engine(), s = E.newState(), id = E.ITEMS_ORDER[0]; s.items[id] = seen(4);
  E.applyLessonResult(s, result(E, id, { touches: { [id]: { ok: 2, bad: 1 } } }));
  assert.equal(s.items[id].lvl, 3); assert.equal(s.items[id].flag, true);
  assert.equal(s.items[id].ok, 4); assert.equal(s.items[id].bad, 1);
  assert.equal(s.items[id].dueAt, NOW);
});
test('verse ids never contaminate vocabulary review records', () => {
  const E = engine(), s = E.newState();
  E.applyLessonResult(s, result(E, 'not-a-vocabulary-id'));
  assert.equal(Object.keys(s.items).length, 0);
});
test('legacy ghost records cannot enter due or filler review queues', () => {
  const E = engine(), s = E.newState(); s.items.ghost = seen(0, 0);
  assert.equal(E.dueIds(s, NOW).length, 0); assert.equal(E.reviewQueue(s, NOW).length, 0);
  assert.equal(E.seenCount(s), 0);
});
test('unknown lesson is locked instead of throwing', () => {
  const E = engine(); assert.equal(E.isUnlocked(E.newState(), 'missing'), false);
});
test('later lessons require the preceding path', () => {
  const E = engine(), s = E.newState();
  assert.equal(E.isUnlocked(s, 'u1l1'), true); assert.equal(E.isUnlocked(s, 'u1l2'), false);
  s.lessons.u1l1 = { done: true }; assert.equal(E.isUnlocked(s, 'u1l2'), true);
});
test('an invalid lesson result is rejected before any state mutation', () => {
  const E = engine(), s = E.newState(), before = JSON.stringify(s);
  assert.throws(() => E.applyLessonResult(s, result(E, undefined, { lessonId: 'missing' })));
  assert.equal(JSON.stringify(s), before);
});
test('all bilingual lesson cards are answerable and build tokens reproduce text', () => {
  const E = engine(); let n = 0;
  for (const lang of ['en', 'bn']) for (const id of Object.keys(E.LESSONS)) {
    const s = E.newState(); s.lang = lang;
    for (const c of E.compileLesson(id, s, { tts: () => false })) {
      n++;
      if (c.options) { assert.ok(c.options.length >= 2, `${id}/${c.t}`); assert.ok(c.options[c.answer]); assert.equal(new Set(c.options).size, c.options.length); assert.equal(E.check(c, c.answer), true); }
      if (c.t === 'build') { const pool = [...c.tiles]; for (const tk of c.verse.tokens) { const i = pool.indexOf(tk); assert.ok(i >= 0); pool.splice(i, 1); } assert.equal(c.verse.tokens.join(' '), c.answer); }
      assert.notEqual(c.t, 'listen');
    }
  }
  assert.ok(n > 500);
});
test('small valid review sets are compilable without fake filler words', () => {
  const E = engine(), s = E.newState(), id = E.ITEMS_ORDER[0]; s.items[id] = seen(0, NOW);
  const cards = E.compileReview(s, [id], { tts: () => false });
  assert.ok(cards.some(c => c.id === id)); assert.equal(cards.at(-1).t, 'complete');
});
test('duplicate and unknown review ids are filtered before compilation', () => {
  const E = engine(), s = E.newState(), id = E.ITEMS_ORDER[0];
  const cards = E.compileReview(s, [id, id, 'ghost'], { tts: () => false });
  assert.equal(cards.filter(c => c.t === 'mc').length, 1);
});
