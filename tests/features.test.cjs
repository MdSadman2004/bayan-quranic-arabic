/* Feature-engine tests: flow combo, Barakah chests, forge, whisper boss, seeds,
 * rite windows, illumination map and ḥalaqah. The engine is pure logic, so each
 * rule is asserted directly with a seeded RNG.
 */
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const ROOT = path.resolve(__dirname, '..');
const NOW = new Date(2026, 9, 15, 12, 0, 0).getTime(); // Thursday noon, local time

function seeded(seed) {
  let state = seed >>> 0;
  return function rand() {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 4294967296;
  };
}

function boot() {
  const context = vm.createContext({
    window: {},
    Date,
    console,
    localStorage: { getItem: () => null, setItem() {} },
  });
  for (const file of ['data', 'engine', 'features']) {
    vm.runInContext(fs.readFileSync(path.join(ROOT, `web/js/${file}.js`), 'utf8'), context, { filename: file });
  }
  return context.window.Bayan;
}

const B = boot();
const F = B.F;

test('flow multiplier follows the declared ladder and caps at the top step', () => {
  assert.equal(F.flowMultiplier(0), 1);
  assert.equal(F.flowMultiplier(1), 2);
  assert.equal(F.flowMultiplier(3), 4);
  assert.equal(F.flowMultiplier(6), 8);
  assert.equal(F.flowMultiplier(99), 8, 'must not exceed the top multiplier');
  assert.equal(F.flowSemitones(1), 0);
  assert.equal(F.flowSemitones(4), 5);
  assert.equal(F.flowXp(10, 4), 50);
});

test('combo only keeps counting inside its time window and resets on a miss', () => {
  let state = { streak: 0, lastAt: 0 };
  state = F.comboTick(state, true, NOW, 8);
  assert.equal(state.streak, 1);
  assert.equal(state.multiplier, 2);
  const inside = F.comboTick(state, true, NOW + 3000, 8);
  assert.equal(inside.streak, 2);
  const outside = F.comboTick(state, true, NOW + 9000, 8);
  assert.equal(outside.streak, 1, 'a slow answer restarts flow');
  const miss = F.comboTick(state, false, NOW + 500, 8);
  assert.equal(miss.streak, 0);
  assert.equal(miss.multiplier, 1);
});

test('chest rolls respect the drop chance, rarity pool and perfect-run bonus', () => {
  const never = F.rollChest({ total: 10, mistakes: 1 }, () => 0.999);
  assert.equal(never, null, 'a roll above the chance yields nothing');
  const always = F.rollChest({ total: 10, mistakes: 1 }, seeded(7));
  assert.ok(always && always.kind, 'a low roll yields loot');
  assert.ok(['relic', 'scroll', 'shield'].includes(always.kind));
  const perfect = F.rollChest({ total: 10, mistakes: 0 }, seeded(11));
  assert.ok(perfect, 'a perfect run always drops');
  const emptyRun = F.rollChest({ total: 0, mistakes: 0 }, () => 0);
  assert.equal(emptyRun, null, 'a run with no answers is not a lesson');
});

test('loot accumulates into the collection and shields are consumable', () => {
  const state = { chests: {}, collection: [] };
  F.applyLoot(state, { id: 'relic_rahm', kind: 'relic' });
  F.applyLoot(state, { id: 'relic_rahm', kind: 'relic' });
  F.applyLoot(state, { id: 'shield_streak', kind: 'shield' });
  assert.equal(state.chests.relic_rahm, 2);
  assert.equal(state.shields, 1);
  assert.deepEqual(state.collection, ['relic_rahm', 'shield_streak'], 'collection lists each item once');
  assert.equal(F.shieldAvailable(state), true);
});

test('a missed day spends a shield before breaking the streak', () => {
  const today = B.E.dayKey(NOW);
  const yesterday = B.E.dayKey(NOW - 86400000);
  const threeDaysAgo = B.E.dayKey(NOW - 3 * 86400000);
  const kept = F.resolveStreakGap({ day: yesterday, streak: 5, shields: 1 }, today, yesterday);
  assert.equal(kept.usedShield, false, 'yesterday is not a gap');
  const shielded = { day: threeDaysAgo, streak: 5, shields: 1 };
  const covered = F.resolveStreakGap(shielded, today, yesterday);
  assert.equal(covered.usedShield, true);
  assert.equal(covered.streak, 5);
  assert.equal(shielded.shields, 0, 'the shield is spent');
  const broken = F.resolveStreakGap({ day: threeDaysAgo, streak: 5, shields: 0 }, today, yesterday);
  assert.equal(broken.streak, 0);
  assert.equal(broken.usedShield, false);
});

test('forge rounds offer the moulded word among unique options', () => {
  for (let seed = 1; seed <= 25; seed++) {
    const round = F.forgeRound(seeded(seed));
    assert.ok(round, 'a round is produced');
    assert.ok(round.root.split(' ').length === 3);
    assert.equal(round.options[round.answer], round.word, 'the answer index points at the forged word');
    assert.equal(new Set(round.options).size, round.options.length, 'options are unique');
    assert.ok(round.options.length >= 3);
  }
});

test('boss waves shuffle without loss and scoring rewards hits, not time alone', () => {
  const queue = F.bossQueue(seeded(3), 5);
  assert.equal(queue.length, 5);
  assert.equal(new Set(queue.map((w) => w.id)).size, 5, 'no duplicated wave');
  const perfect = F.bossScore(queue, 5, 20);
  assert.equal(perfect.hits, 5);
  assert.equal(perfect.shattered, true);
  assert.equal(perfect.shields, 1);
  assert.ok(perfect.xp > 0);
  const sloppy = F.bossScore(queue, 2, 20);
  assert.equal(sloppy.shattered, false);
  assert.equal(sloppy.shields, 0);
  assert.ok(sloppy.xp < perfect.xp);
  const idle = F.bossScore(queue, 0, 45);
  assert.equal(idle.accuracy, 0);
  assert.ok(idle.xp <= Math.round(45 / 5), 'time alone cannot outscore hits');
});

test('seeds are keyed to learned roots and the daily whisper is stable per day', () => {
  const allRoots = [...new Set(F.raw.seeds.items.map((s) => s.root))];
  const found = F.seedsForRoots(allRoots);
  assert.equal(found.length, F.raw.seeds.items.length);
  assert.equal(F.seedsForRoots([]).length, 0);
  assert.equal(F.seedsForRoots(['no-such-root']).length, 0);
  const first = F.whisperFor(NOW, 0);
  assert.equal(first.id, F.whisperFor(NOW + 60000, 0).id, 'stable within the same day');
  assert.notEqual(first.id, F.whisperFor(NOW + 86400000, 0).id, 'changes the next day');
  assert.ok(F.loaderFor('qalam_stroke'));
  assert.equal(F.loaderFor('nope'), null);
});

test('the sky tracks the hour and the primer tracks the coming prayer', () => {
  const at = (hour) => new Date(2026, 9, 15, hour, 30, 0);
  assert.equal(F.skyForDate(at(5)), 'dawn');
  assert.equal(F.skyForDate(at(13)), 'noon');
  assert.equal(F.skyForDate(at(19)), 'dusk');
  assert.equal(F.skyForDate(at(23)), 'night');
  assert.equal(F.skyClass(at(5)), 'sky-dawn');
  assert.equal(F.waqtWindow(at(5)).id, 'fajr');
  assert.equal(F.waqtWindow(at(13)).id, 'dhuhr');
  assert.equal(F.waqtWindow(at(16)).id, 'asr');
  assert.equal(F.waqtWindow(at(19)).id, 'maghrib');
  assert.equal(F.waqtWindow(at(22)).id, 'isha');
  assert.equal(F.jumuahOpen(new Date(2026, 9, 16)), true, 'Friday opens the quest');
  assert.equal(F.jumuahOpen(new Date(2026, 9, 15)), false);
});

test('every rite module carries bilingual copy and linked verses', () => {
  assert.ok(F.salahSteps().length >= 5);
  for (const step of F.salahSteps()) {
    assert.ok(step.ar && step.en && step.bn && step.grammar.en && step.grammar.bn, step.id);
  }
  for (const window of F.raw.rites.waqt.windows) {
    const verse = B.E.ITEMS[window.verse];
    assert.ok(verse || /v_u\d|v_fatiha|v_kursi|v_asr/.test(window.verse), window.id);
  }
  for (const phrase of F.jumuahPhrases()) assert.ok(phrase.ar && phrase.en && phrase.bn);
  for (const item of F.wuduAdhanItems().concat(F.zakatHajjItems())) {
    assert.ok(item.term && item.body.en && item.body.bn, item.id);
  }
  assert.ok(F.sawmDrills().length >= 2);
});

test('the illumination map lights only verses sharing a learned root', () => {
  const empty = F.illumination({ items: {} });
  assert.equal(empty.litVerses, 0);
  assert.equal(empty.percent, 0);
  assert.ok(empty.totalVerses >= 30, 'the bundled corpus is counted');

  const root = 'ع ل م';
  const learned = Object.keys(B.E.ITEMS).filter((id) => B.E.ITEMS[id].root === root);
  assert.ok(learned.length > 0, 'the corpus teaches this root');
  const state = { items: {} };
  learned.forEach((id) => { state.items[id] = { lvl: 2, seen: 3 }; });
  const partial = F.illumination(state);
  assert.ok(partial.rootCount >= 1);
  assert.ok(partial.litVerses > 0, 'verses carrying the root are lit');
  assert.ok(partial.percent > 0 && partial.percent <= 100);
  for (const verse of partial.verses) {
    if (verse.lit) assert.ok(verse.litTokens > 0);
  }
  assert.equal(F.carriesRoot('ع ل م', 'يَعْلَمُونَ'), true);
  assert.equal(F.carriesRoot('ع ل م', 'الْكِتَابُ'), false);
  assert.equal(F.carriesRoot('', 'anything'), false);
});

test('ḥalaqah respects the member cap and can lend a shield', () => {
  const circle = F.newCircle(3);
  F.circleAdd(circle, 'Amina');
  F.circleAdd(circle, 'Amina');
  assert.equal(circle.members.length, 1, 'no duplicate members');
  assert.equal(circle.members[0], 'Amina');
  F.circleAdd(circle, 'Yusuf');
  F.circleAdd(circle, 'Bilal');
  F.circleAdd(circle, 'Over');
  assert.equal(circle.members.length, 3, 'the cap holds');
  F.circleAdd(circle, '   ');
  assert.equal(circle.members.length, 3);
  const state = {};
  circle.lantern = 0;
  assert.equal(F.duaShield(circle, state), true);
  assert.ok(circle.lantern > 0);
  assert.equal(state.shields, 1);
  circle.lantern = 1;
  assert.equal(F.duaShield(circle, state), false, 'a full lantern needs no shield');
  F.circleTick(circle, true);
  assert.equal(circle.lantern, 1, 'light never exceeds one');
  F.circleTick(circle, false);
  assert.ok(circle.lantern < 1, 'a missed day dims shared light');
});
