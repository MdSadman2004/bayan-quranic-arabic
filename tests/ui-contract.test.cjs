/* UI contract test: every string key the web client asks for must exist in both
 * languages, and the feature payload must cover every screen that renders it.
 * Catches the classic "key exists in English only" and "screen shipped with a
 * raw key on it" failures without needing a browser.
 */
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const ROOT = path.resolve(__dirname, '..');
const WEB = path.join(ROOT, 'web');
const SOURCES = ['js/app.js', 'js/features-ui.js', 'js/loaders.js', 'js/offline.js'];

function read(rel) {
  return fs.readFileSync(path.join(WEB, rel), 'utf8');
}

function keysUsedBy(source) {
  const keys = new Set();
  // Literals that look like keys but are not UI strings: the storage key and
  // asset filenames. Everything else must resolve in the string table.
  const NOT_UI_STRINGS = new Set(['bayan.v1', 'index.html', 'offline-assets.json']);
  const patterns = [
    /\bt\(\s*"(?:[^"]*)"?\s*,\s*"([^"]+)"/g, // t("key", ...)
    /\bt\(\s*"([^"]+)"/g,                    // t("key")
    /STR\(\s*"([^"]+)"/g,
    /"([a-z][a-z0-9]*\.[a-z0-9_.]+)"/g,      // any "group.key" literal
  ];
  for (const pattern of patterns) {
    let match;
    while ((match = pattern.exec(source)) !== null) {
      const key = match[1];
      if (!/^[a-z][a-z0-9]*\.[a-z0-9_.]+$/.test(key)) continue;
      if (NOT_UI_STRINGS.has(key)) continue;
      if (/\.(js|css|json|html|svg|png|woff2|webmanifest)$/.test(key)) continue;
      keys.add(key);
    }
  }
  return keys;
}

function loadStrings() {
  const context = vm.createContext({ window: {}, console });
  vm.runInContext(read('js/i18n.js'), context, { filename: 'i18n.js' });
  return context.window.Bayan.STR;
}

test('every UI string key resolves in English and Bangla', () => {
  const STR = loadStrings();
  const missing = [];
  for (const rel of SOURCES) {
    for (const key of keysUsedBy(read(rel))) {
      const en = STR(key, 'en');
      const bn = STR(key, 'bn');
      if (en === key) missing.push(`${rel}: ${key} (missing)`);
      else if (bn === en) missing.push(`${rel}: ${key} (untranslated in bn)`);
    }
  }
  assert.deepEqual(missing, [], `untranslated or missing keys: ${missing.join('; ')}`);
});

test('the feature payload backs every new screen', () => {
  const context = vm.createContext({ window: {}, console, Date, localStorage: { getItem: () => null, setItem() {} } });
  for (const file of ['js/data.js', 'js/engine.js', 'js/features.js']) {
    vm.runInContext(read(file), context, { filename: file });
  }
  const F = context.window.Bayan.F;
  assert.equal(F.available, true, 'features payload is bundled');
  const raw = F.raw;
  for (const key of ['combo', 'chests', 'forge', 'boss', 'seeds', 'rites', 'whispers', 'loaders', 'halaqah', 'illumination']) {
    assert.ok(raw[key], `feature payload missing ${key}`);
  }
  assert.ok(raw.forge.moulds.length >= 2);
  assert.ok(raw.boss.waves.length >= 4);
  assert.ok(raw.seeds.items.length >= 6);
  assert.deepEqual(
    Object.keys(raw.rites).sort(),
    ['jumuah', 'salah', 'sawm', 'waqt', 'wuduAdhan', 'zakatHajj'],
    'all six rite modules shipped'
  );
  assert.equal(raw.loaders.length, 6, 'six loader definitions shipped');
});

test('index.html loads the feature scripts in dependency order', () => {
  const html = read('index.html');
  const order = ['js/data.js', 'js/i18n.js', 'js/storage.js', 'js/engine.js', 'js/audio.js',
    'js/features.js', 'js/loaders.js', 'js/features-ui.js', 'js/app.js', 'js/offline.js'];
  let cursor = -1;
  for (const file of order) {
    const at = html.indexOf(file);
    assert.ok(at > -1, `${file} is not loaded`);
    assert.ok(at > cursor, `${file} is out of order`);
    cursor = at;
  }
});

test('the offline manifest covers the feature code it ships', () => {
  const manifest = JSON.parse(read('offline-assets.json'));
  const urls = manifest.files.map((entry) => entry.url);
  for (const file of ['js/features.js', 'js/features-ui.js', 'js/loaders.js']) {
    assert.ok(urls.includes(file), `${file} must be cached for offline use`);
  }
  const sw = read('sw.js');
  assert.doesNotMatch(sw, /__FILES__|__VERSION__/);
  for (const file of ['js/features.js', 'js/features-ui.js', 'js/loaders.js']) {
    assert.ok(sw.includes(file), `service worker must cache ${file}`);
  }
});

test('every feature API the UI calls actually exists', () => {
  // app.js and features-ui.js drive the feature engine through window.Bayan.F.
  // A renamed export turned into an uncatchable runtime TypeError on the
  // lesson-complete screen once; this asserts the call surface instead.
  const feat = read('js/features.js');
  const exportBlock = feat.match(/BAYAN\.F\s*=\s*\{([\s\S]*?)\n  \};/);
  assert.ok(exportBlock, 'features.js exposes BAYAN.F');
  const exports = new Set([...exportBlock[1].matchAll(/([A-Za-z_][A-Za-z0-9_]*)\s*:/g)].map((m) => m[1]));
  assert.ok(exports.size >= 20, `expected a broad export surface, saw ${exports.size}`);
  const missing = [];
  for (const rel of ['js/app.js', 'js/features-ui.js']) {
    const source = read(rel);
    for (const match of source.matchAll(/\bF\.([A-Za-z_][A-Za-z0-9_]*)/g)) {
      if (!exports.has(match[1])) missing.push(`${rel}: F.${match[1]}`);
    }
  }
  assert.deepEqual([...new Set(missing)], [], `calls into a missing feature API: ${missing.join('; ')}`);
});
