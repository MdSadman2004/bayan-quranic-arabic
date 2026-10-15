/* Bayan — feature screens: flow combo, Barakah chest reveal, Sarf forge card,
 * whisper boss battle, wisdom seeds, the rite modules (ṣalāh, waqt, wuḍūʾ/adhan,
 * Jumuʿah, ṣawm, zakāh/ḥajj), the illumination map and the ḥalaqah circle.
 * Rendering only; every rule comes from Bayan.F (features.js).
 */
(function () {
  "use strict";
  const BAYAN = window.Bayan || {};
  const F = BAYAN.F;
  const L = BAYAN.Loaders;
  const STR = BAYAN.STR;
  const esc = (value) => String(value == null ? "" : value)
    .replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));

  let bossState = null;
  let salahIndex = 0;
  let circleDraft = "";

  const t = (api, key, vars) => STR(key, api.lang(), vars);
  const pick = (api, node) => (node && (node[api.lang()] || node.en)) || "";
  /* Drawn icons, matching the app's existing stroke/weight language. */
  const ICON = {
    crescent: '<svg viewBox="0 0 24 24" class="i"><path d="M14 3a9 9 0 1 0 7 14A7.5 7.5 0 0 1 14 3z"/></svg>',
    arch: '<svg viewBox="0 0 24 24" class="i"><path d="M12 2c3 3 5 4.6 5 7.5V21h-4v-5a1 1 0 0 0-2 0v5H7V9.5C7 6.6 9 5 12 2z"/></svg>',
    lantern: '<svg viewBox="0 0 24 24" class="i"><path d="M9 2h6v2H9zm-1 3h8l1 4v9a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V9z"/></svg>'
  };

  /* ------------------------------------------------------------- flow (1) */
  function flowBonus(xp, maxCombo) {
    const multiplier = F.flowMultiplier(maxCombo);
    return { multiplier: multiplier, total: Math.round(xp * multiplier), extra: Math.round(xp * multiplier) - xp };
  }
  function flowChip(run) {
    if (!run || !run.flow || run.flow.multiplier <= 1) return "";
    return '<span class="flow-chip show">' + run.flow.multiplier + "×</span>";
  }

  /* ------------------------------------------------------- forge card (3) */
  function injectForge(cards, lesson, rand) {
    if (!cards || !lesson) return cards;
    const roots = new Set();
    (lesson.resolvedItems || []).forEach((item) => { if (item && item.root) roots.add(item.root); });
    if (roots.size < 2) return cards;
    const round = F.forgeRound(rand || Math.random);
    if (!round) return cards;
    const at = cards.findIndex((card) => card.t === "complete");
    cards.splice(at < 0 ? cards.length : at, 0, { t: "forge", id: "forge:" + round.mould.id, round: round });
    return cards;
  }
  function forgeCard(card, api) {
    const round = card.round;
    const options = round.options.map((word, index) =>
      '<button class="forge-option ar" dir="rtl" lang="ar" data-fui="forge-pick" data-index="' + index + '">' + esc(word) + "</button>"
    ).join("");
    const graded = card.graded;
    return '<div class="card card-forge">' +
      '<div class="card-tag forge">' + esc(t(api, "forge.tag")) + "</div>" +
      '<div class="forge-mould"><span class="forge-pattern ar" dir="rtl" lang="ar">' + esc(round.mould.pattern) + "</span>" +
      '<span class="forge-role">' + esc(pick(api, round.mould.role)) + "</span></div>" +
      '<div class="forge-root ar" dir="rtl" lang="ar">' + esc(round.root) + "</div>" +
      '<div class="q-label">' + esc(t(api, "forge.ask")) + "</div>" +
      '<div class="forge-options">' + options + "</div>" +
      (graded ? '<div class="forge-answer">' + esc(pick(api, round.meaning)) + "</div>" : "") +
      "</div>";
  }
  function forgePick(api, card, word) {
    if (!card || card.graded) return;
    const ok = word === card.round.word;
    card.graded = { ok: ok };
    card.selectedWord = word;
    api.record("forge:" + card.round.mould.id, ok);
    api.render();
  }

  /* ------------------------------------------------------ chest reveal (2) */
  function chestReveal(chest, api) {
    if (!chest) return "";
    const rarity = (F.raw.chests.rarities || []).find((entry) => entry.id === chest.rarity) || { label: { en: "", bn: "" } };
    return '<div class="chest-reveal rarity-' + esc(chest.rarity) + '">' +
      '<div class="chest-box" aria-hidden="true"><span class="chest-lid"></span><span class="chest-body"></span></div>' +
      '<div class="chest-meta"><b>' + esc(pick(api, chest.title)) + "</b>" +
      '<small>' + esc(pick(api, rarity.label)) + " · " + esc(t(api, "chest." + chest.kind)) + "</small></div>" +
      '<p class="chest-body">' + esc(pick(api, chest.body)) + "</p></div>";
  }

  /* ------------------------------------------------------- seeds (7–12) */
  function seedCards(seeds, api) {
    if (!seeds || !seeds.length) return "";
    return seeds.map((seed) =>
      '<article class="seed-card kind-' + esc(seed.kind) + '">' +
      '<div class="seed-head"><span class="seed-kind">' + esc(t(api, "seed." + seed.kind)) + "</span>" +
      '<span class="seed-word ar" dir="rtl" lang="ar">' + esc(seed.word) + "</span></div>" +
      "<b>" + esc(pick(api, seed.title)) + "</b>" +
      "<p>" + esc(pick(api, seed.body)) + "</p>" +
      (seed.kind === "tazkiyah" ? '<button class="btn btn-ghost btn-sm" data-fui="seed-done">' + esc(t(api, "seed.done")) + "</button>" : "") +
      "</article>"
    ).join("");
  }

  /* -------------------------------------------------------- boss view (4) */
  function beginBoss(api) {
    const waves = F.bossQueue(Math.random, 8);
    bossState = { waves: waves, index: 0, hits: 0, wrong: 0, endsAt: Date.now() + F.bossSeconds() * 1000, timer: null, done: false };
    return bossState;
  }
  function bossView(api) {
    if (!bossState) beginBoss(api);
    const boss = F.raw.boss;
    if (bossState.done) {
      const score = F.bossScore(bossState.waves, bossState.hits, 0);
      return '<div class="pane">' +
        '<h1 class="display">' + esc(pick(api, boss.name)) + "</h1>" +
        '<div class="boss-result"><b>' + score.hits + "/" + score.total + "</b>" +
        "<span>" + esc(t(api, "boss.hits")) + "</span></div>" +
        '<p class="muted">' + esc(t(api, "boss.resultXp", { n: score.xp })) + "</p>" +
        (score.shields ? '<p class="muted">' + esc(t(api, "boss.shieldWon")) + "</p>" : "") +
        '<button class="btn btn-accent btn-block" id="boss-again" data-fui="boss-start">' + esc(t(api, "boss.again")) + "</button>" +
        '<button class="btn btn-ghost btn-block" data-fui="nav-home">' + esc(t(api, "common.continue")) + "</button>" +
        "</div>";
    }
    const wave = bossState.waves[bossState.index];
    const remaining = Math.max(0, Math.ceil((bossState.endsAt - Date.now()) / 1000));
    const options = wave.options.map((option, index) =>
      '<button class="opt boss-opt" data-fui="boss-answer" data-index="' + index + '">' + esc(pick(api, option)) + "</button>").join("");
    return '<div class="pane boss">' +
      '<div class="boss-hud"><span class="boss-clock">' + remaining + "s</span>" +
      '<span class="boss-progress">' + (bossState.index + 1) + "/" + bossState.waves.length + "</span></div>" +
      '<div class="boss-trap"><div class="q-label">' + esc(t(api, "boss.trap")) + "</div>" +
      '<p class="boss-whisper">' + esc(pick(api, wave.trap)) + "</p></div>" +
      '<div class="opts">' + options + "</div>" +
      '<p class="muted small">' + esc(pick(api, boss.note)) + "</p></div>";
  }
  function bossAnswer(api, index) {
    if (!bossState || bossState.done) return;
    const wave = bossState.waves[bossState.index];
    const option = wave.options[index];
    const correct = index === wave.answer;
    if (correct) bossState.hits += 1; else bossState.wrong += 1;
    api.record("boss:" + wave.id, correct);
    bossState.index += 1;
    if (bossState.index >= bossState.waves.length || Date.now() >= bossState.endsAt) finishBoss(api);
    else api.render();
  }
  function finishBoss(api) {
    if (!bossState || bossState.done) return;
    bossState.done = true;
    const secondsLeft = Math.max(0, Math.round((bossState.endsAt - Date.now()) / 1000));
    const score = F.bossScore(bossState.waves, bossState.hits, secondsLeft);
    api.award(score.xp, score.shields, "boss");
    api.render();
  }
  function bossTick(api) {
    if (!bossState || bossState.done) {
      if (bossState && bossState.timer) { clearInterval(bossState.timer); bossState.timer = null; }
      return;
    }
    if (Date.now() >= bossState.endsAt) { finishBoss(api); return; }
    if (api.currentView() === "boss") api.render();
  }
  function clearBoss() {
    if (bossState && bossState.timer) clearInterval(bossState.timer);
    if (bossState) bossState.timer = null;
  }

  /* ------------------------------------------------------- rites view (13–18) */
  function salahHtml(api) {
    const steps = F.salahSteps();
    const step = steps[salahIndex] || steps[0];
    const dots = steps.map((entry, index) =>
      '<button class="rite-dot ' + (index === salahIndex ? "on" : "") + '" data-fui="salah-go" data-index="' + index + '" aria-label="' + esc(pick(api, entry.posture)) + '"></button>').join("");
    return '<section class="rite">' +
      '<h2 class="sect">' + esc(pick(api, F.raw.rites.salah.name)) + "</h2>" +
      '<p class="muted">' + esc(pick(api, F.raw.rites.salah.note)) + "</p>" +
      '<div class="salah-card">' +
      '<div class="salah-posture">' + esc(pick(api, step.posture)) + "</div>" +
      '<div class="ar ar-lg" dir="rtl" lang="ar">' + esc(step.ar) + "</div>" +
      '<div class="salah-meaning">' + esc(step[api.lang()] || step.en) + "</div>" +
      '<p class="salah-grammar">' + esc(pick(api, step.grammar)) + "</p></div>" +
      '<div class="rite-dots">' + dots + "</div>" +
      '<div class="row rite-nav">' +
      '<button class="btn btn-ghost btn-sm" data-fui="salah-step" data-delta="-1">' + esc(t(api, "rite.prev")) + "</button>" +
      '<button class="btn btn-ink btn-sm" data-fui="salah-step" data-delta="1">' + esc(t(api, "rite.next")) + "</button>" +
      "</div></section>";
  }
  function waqtHtml(api) {
    const current = F.waqtWindow(new Date());
    const windows = F.raw.rites.waqt.windows;
    return '<section class="rite">' +
      '<h2 class="sect">' + esc(pick(api, F.raw.rites.waqt.name)) + "</h2>" +
      '<p class="muted">' + esc(pick(api, F.raw.rites.waqt.note)) + "</p>" +
      '<p class="muted small">' + esc(t(api, "waqt.approx")) + "</p>" +
      '<div class="waqt-list">' + windows.map((window) =>
        '<button class="waqt-row ' + (window.id === current.id ? "on" : "") + '" data-fui="waqt-open" data-verse="' + esc(window.verse) + '">' +
        '<span class="waqt-name">' + esc(pick(api, window.name)) + "</span>" +
        '<span class="waqt-focus">' + esc(pick(api, window.focus)) + "</span></button>").join("") +
      "</div></section>";
  }
  function wuduHtml(api) {
    const items = F.wuduAdhanItems();
    return '<section class="rite"><h2 class="sect">' + esc(pick(api, F.raw.rites.wuduAdhan.name)) + "</h2>" +
      '<p class="muted">' + esc(pick(api, F.raw.rites.wuduAdhan.note)) + "</p>" +
      '<div class="term-grid">' + items.map((item) =>
        '<article class="term-card"><div class="term-head"><span class="term-word ar" dir="rtl" lang="ar">' + esc(item.term) + "</span>" +
        '<span class="term-root ar" dir="rtl" lang="ar">' + esc(item.root) + "</span></div>" +
        "<b>" + esc(pick(api, item.title)) + "</b><p>" + esc(pick(api, item.body)) + "</p></article>").join("") +
      "</div></section>";
  }
  function jumuahHtml(api) {
    const open = F.jumuahOpen(new Date());
    return '<section class="rite">' +
      '<h2 class="sect">' + esc(pick(api, F.raw.rites.jumuah.name)) + "</h2>" +
      '<p class="muted">' + esc(pick(api, F.raw.rites.jumuah.note)) + "</p>" +
      '<p class="jumuah-state ' + (open ? "open" : "closed") + '">' + esc(t(api, open ? "jumuah.open" : "jumuah.closed")) + "</p>" +
      '<p class="muted small">' + esc(F.raw.rites.jumuah.surahRef) + "</p>" +
      '<div class="term-grid">' + F.jumuahPhrases().map((phrase) =>
        '<article class="term-card"><div class="ar" dir="rtl" lang="ar">' + esc(phrase.ar) + "</div><p>" +
        esc(phrase[api.lang()] || phrase.en) + "</p>" +
        '<button class="btn btn-ghost btn-sm" data-fui="say" data-text="' + esc(phrase.ar) + '">' + esc(t(api, "lesson.play")) + "</button></article>").join("") +
      "</div></section>";
  }
  function sawmHtml(api) {
    const drills = F.sawmDrills();
    const listen = api.canListen();
    const body = listen
      ? '<div class="term-grid">' + drills.map((drill) =>
          '<article class="term-card"><p>' + esc(pick(api, drill.ask)) + "</p>" +
          '<button class="btn btn-ghost btn-sm" data-fui="sawm" data-drill="' + esc(drill.id) + '">' +
          esc(t(api, "sawm.drill")) + "</button>" +
          '<div class="sawm-options" data-options="' + esc(drill.id) + '"></div></article>').join("") + "</div>"
      : '<p class="muted">' + esc(t(api, "sawm.needsVoice")) + "</p>";
    return '<section class="rite"><h2 class="sect">' + esc(pick(api, F.raw.rites.sawm.name)) + "</h2>" +
      '<p class="muted">' + esc(pick(api, F.raw.rites.sawm.note)) + "</p>" + body + "</section>";
  }
  function zakatHajjHtml(api) {
    return '<section class="rite"><h2 class="sect">' + esc(pick(api, F.raw.rites.zakatHajj.name)) + "</h2>" +
      '<p class="muted">' + esc(pick(api, F.raw.rites.zakatHajj.note)) + "</p>" +
      '<div class="term-grid">' + F.zakatHajjItems().map((item) =>
        '<article class="term-card"><div class="term-head"><span class="term-word ar" dir="rtl" lang="ar">' + esc(item.term) + "</span>" +
        '<span class="term-root ar" dir="rtl" lang="ar">' + esc(item.root) + "</span></div>" +
        "<b>" + esc(pick(api, item.title)) + "</b><p>" + esc(pick(api, item.body)) + "</p></article>").join("") +
      "</div></section>";
  }
  function ritesView(api) {
    return '<div class="pane rites">' +
      '<h1 class="display">' + esc(t(api, "rites.title")) + "</h1>" +
      '<p class="muted">' + esc(t(api, "rites.note")) + "</p>" +
      salahHtml(api) + waqtHtml(api) + wuduHtml(api) + jumuahHtml(api) + sawmHtml(api) + zakatHajjHtml(api) +
      "</div>";
  }

  /* -------------------------------------------- illumination + circle (5,6) */
  function mapHtml(api, state) {
    const map = F.illumination(state);
    const byUnit = {};
    map.verses.forEach((verse) => {
      byUnit[verse.unit] = byUnit[verse.unit] || { lit: 0, total: 0, verses: [] };
      byUnit[verse.unit].total += 1;
      if (verse.lit) byUnit[verse.unit].lit += 1;
      byUnit[verse.unit].verses.push(verse);
    });
    const units = Object.keys(byUnit).map((unitId) => {
      const bucket = byUnit[unitId];
      const unitMeta = (BAYAN.E.UNITS || []).find((unit) => unit.id === unitId) || { title: { en: unitId, bn: unitId }, num: unitId };
      return '<div class="map-unit"><div class="map-unit-head"><span class="map-unit-name">' +
        esc(pick(api, unitMeta.title)) + '</span><span class="map-unit-count">' + bucket.lit + "/" + bucket.total + "</span></div>" +
        '<div class="map-verse-row">' + bucket.verses.map((verse) =>
          '<span class="map-dot ' + (verse.lit ? "lit" : "") + '" title="' + esc(verse.ref) + '" style="--fill:' + verse.fraction.toFixed(2) + '"></span>').join("") +
        "</div></div>";
    }).join("");
    return '<section class="rite"><h2 class="sect">' + esc(pick(api, F.raw.illumination.name)) + "</h2>" +
      '<p class="muted">' + esc(pick(api, F.raw.illumination.note)) + "</p>" +
      '<div class="map-summary"><b>' + map.percent + "%</b><span>" +
      esc(t(api, "map.summary", { lit: map.litVerses, total: map.totalVerses, roots: map.rootCount })) + "</span></div>" +
      '<div class="map-units">' + units + "</div>" +
      '<div class="map-roots">' + map.roots.map((root) =>
        '<span class="root-chip ar" dir="rtl" lang="ar">' + esc(root) + "</span>").join("") + "</div></section>";
  }
  function circleHtml(api, state) {
    const circle = state.circle || F.newCircle();
    const members = (circle.members || []).map((name) =>
      '<span class="member-chip">' + esc(name) + "</span>").join("");
    const lantern = Math.round((circle.lantern || 0) * 100);
    return '<section class="rite"><h2 class="sect">' + esc(pick(api, F.raw.halaqah.name)) + "</h2>" +
      '<p class="muted">' + esc(pick(api, F.raw.halaqah.note)) + "</p>" +
      '<div class="lantern-card"><div class="lantern" style="--light:' + lantern + '%"><span></span></div>' +
      '<div class="lantern-body"><b>' + lantern + "%</b><small>" + esc(t(api, "circle.light")) + "</small>" +
      '<div class="members">' + (members || '<span class="muted small">' + esc(t(api, "circle.empty")) + "</span>") + "</div>" +
      '<div class="circle-actions">' +
      '<input type="text" id="circle-name" maxlength="24" value="' + esc(circleDraft) + '" placeholder="' + esc(t(api, "circle.name")) + '" autocomplete="off">' +
      '<button class="btn btn-ghost btn-sm" data-fui="circle-add">' + esc(t(api, "circle.add")) + "</button>" +
      '<button class="btn btn-ink btn-sm" data-fui="circle-shield">' + esc(t(api, "circle.shield")) + "</button>" +
      "</div><p class='muted small'>" + esc(t(api, "circle.shields", { n: state.shields || 0 })) + "</p></div></div></section>";
  }
  function circleView(api, state) {
    return '<div class="pane circle"><h1 class="display">' + esc(t(api, "circle.title")) + "</h1>" +
      '<p class="muted">' + esc(t(api, "circle.note")) + "</p>" +
      circleHtml(api, state) + mapHtml(api, state) +
      '<section class="rite"><h2 class="sect">' + esc(t(api, "collection.title")) + "</h2>" +
      collectionHtml(api, state) + "</section></div>";
  }
  function collectionHtml(api, state) {
    const chests = state.chests || {};
    const ids = Object.keys(chests);
    const relics = (F.raw.chests.loot || []).filter((entry) => ids.indexOf(entry.id) >= 0);
    if (!relics.length) return '<p class="muted">' + esc(t(api, "collection.empty")) + "</p>";
    return '<div class="term-grid">' + relics.map((entry) =>
      '<article class="term-card"><b>' + esc(pick(api, entry.title)) + "</b><small>" +
      esc(t(api, "chest." + entry.kind)) + " × " + chests[entry.id] + "</small><p>" + esc(pick(api, entry.body)) + "</p></article>").join("") + "</div>";
  }

  /* ------------------------------------------------------- home additions */
  function homeExtras(api, state) {
    const waqt = F.waqtWindow(new Date());
    const circle = state.circle;
    const jumuah = F.jumuahOpen(new Date());
    const whisper = F.whisperFor(Date.now(), 0);
    let html = '<div class="home-extras">';
    html += '<button class="extras-card" data-fui="waqt-open" data-verse="' + esc(waqt.verse) + '">' +
      '<span class="extras-icon">' + ICON.crescent + '</span><span class="extras-body"><b>' +
      esc(t(api, "home.primer", { name: pick(api, waqt.name) })) + "</b><small>" + esc(pick(api, waqt.focus)) + "</small></span></button>";
    if (jumuah) {
      html += '<button class="extras-card jumuah" data-fui="nav-rites"><span class="extras-icon">' + ICON.arch + '</span>' +
        '<span class="extras-body"><b>' + esc(t(api, "home.jumuah")) + "</b><small>" +
        esc(pick(api, F.raw.rites.jumuah.name)) + "</small></span></button>";
    }
    if (circle && circle.members && circle.members.length) {
      html += '<button class="extras-card" data-fui="nav-circle"><span class="extras-icon">' + ICON.lantern + '</span>' +
        '<span class="extras-body"><b>' + esc(t(api, "home.circle", { n: circle.members.length })) + "</b><small>" +
        esc(t(api, "circle.light")) + " " + Math.round((circle.lantern || 0) * 100) + "%</small></span></button>";
    }
    html += "</div>";
    const whisperCard = L.whisperCard(whisper, api.lang(), t(api, "loader.skip"));
    return html + whisperCard;
  }
  function practiceExtras(api, state) {
    const boss = F.raw.boss;
    return '<div class="boss-entry"><div class="boss-invite"><b>' + esc(pick(api, boss.name)) + "</b><small>" +
      esc(t(api, "boss.invite", { n: boss.seconds })) + "</small></div>" +
      '<button class="btn btn-accent" data-fui="boss-start">' + esc(t(api, "boss.start")) + "</button></div>";
  }

  /* ---------------------------------------------------------- lesson glue */
  function lessonLoader(lesson) {
    const first = (lesson.resolvedItems || []).find((item) => item.root);
    if (!first) return L.mushafShimmer(3);
    const mould = (F.raw.forge.moulds || [])[0];
    const family = mould ? mould.examples.filter((example) => example.root === first.root).map((example) => example.word) : [];
    return L.rootSprout(first.root, family, "");
  }
  function completedExtras(api, state, result) {
    let html = "";
    if (result && result.flow && result.flow > 1) {
      html += '<div class="res flow-res"><b>' + result.flow + "×</b><span>" + esc(t(api, "lesson.flow")) + "</span></div>";
    }
    if (result && result.chest) html += chestReveal(result.chest, api);
    if (result && result.seeds && result.seeds.length) html += '<div class="seed-stack">' + seedCards(result.seeds, api) + "</div>";
    return html;
  }

  /* ------------------------------------------------------------- binding */
  function bind(api) {
    const screen = document.getElementById("screen");
    if (!screen || screen.dataset.fuiBound === "1") return;
    screen.dataset.fuiBound = "1";
    screen.addEventListener("click", (event) => {
      const node = event.target.closest("[data-fui]");
      if (!node) return;
      const action = node.dataset.fui;
      const state = api.state();
      if (action === "boss-start") { clearBoss(); beginBoss(api); bossState.timer = setInterval(() => bossTick(api), 1000); api.go("boss"); return; }
      if (action === "boss-answer") { bossAnswer(api, Number(node.dataset.index)); return; }
      if (action === "nav-home") { api.go("home"); return; }
      if (action === "nav-rites") { api.go("rites"); return; }
      if (action === "nav-circle") { api.go("circle"); return; }
      if (action === "forge-pick") { forgePick(api, api.currentCard(), node.textContent.trim()); return; }
      if (action === "salah-go") { salahIndex = Number(node.dataset.index); api.render(); return; }
      if (action === "salah-step") {
        const steps = F.salahSteps();
        salahIndex = Math.max(0, Math.min(steps.length - 1, salahIndex + Number(node.dataset.delta)));
        api.render(); return;
      }
      if (action === "say") { api.speak(node.dataset.text); return; }
      if (action === "sawm") {
        const drill = F.sawmDrills().find((entry) => entry.id === node.dataset.drill);
        if (!drill) return;
        const verse = BAYAN.E.ITEMS[drill.verse] || BAYAN.E.lessonObject("u1l1");
        api.speak((BAYAN.E.itemObject(drill.verse) || verse || {}).ar || "");
        const holder = screen.querySelector('[data-options="' + drill.id + '"]');
        if (holder) {
          holder.innerHTML = drill.options.map((option, index) =>
            '<button class="opt" data-fui="sawm-pick" data-drill="' + esc(drill.id) + '" data-index="' + index + '">' +
            '<span class="ar" dir="rtl" lang="ar">' + esc(option) + "</span></button>").join("");
        }
        return;
      }
      if (action === "sawm-pick") {
        const drill = F.sawmDrills().find((entry) => entry.id === node.dataset.drill);
        const holder = screen.querySelector('[data-options="' + drill.id + '"]');
        const ok = Number(node.dataset.index) === drill.answer;
        if (holder) holder.innerHTML = '<p class="muted">' + esc(ok ? t(api, "sawm.correct") : t(api, "sawm.wrong")) + "</p>";
        api.record("sawm:" + drill.id, ok);
        api.award(ok ? 3 : 0, 0, "rite");
        return;
      }
      if (action === "waqt-open") {
        const verse = BAYAN.E.itemObject(node.dataset.verse);
        if (verse) { api.openVerse(verse); return; }
        return;
      }
      if (action === "circle-add") {
        const input = document.getElementById("circle-name");
        circleDraft = input ? input.value : "";
        state.circle = F.circleAdd(state.circle || F.newCircle(), circleDraft);
        circleDraft = "";
        api.save(); api.render(); return;
      }
      if (action === "circle-shield") {
        state.circle = state.circle || F.newCircle();
        if (F.duaShield(state.circle, state)) { api.save(); api.toast(t(api, "circle.shieldCast")); }
        api.render(); return;
      }
      if (action === "skip-loader" || action === "seed-done") {
        const node2 = screen.querySelector(".ld-whisper");
        if (node2) node2.classList.add("ld-out");
        return;
      }
    });
  }

  BAYAN.FUI = {
    bind: bind,
    flowBonus: flowBonus,
    flowChip: flowChip,
    injectForge: injectForge,
    forgeCard: forgeCard,
    forgePick: forgePick,
    chestReveal: chestReveal,
    seedCards: seedCards,
    homeExtras: homeExtras,
    practiceExtras: practiceExtras,
    ritesView: ritesView,
    circleView: circleView,
    bossView: bossView,
    beginBoss: beginBoss,
    clearBoss: clearBoss,
    bossTick: bossTick,
    lessonLoader: lessonLoader,
    completedExtras: completedExtras,
    resetSalah: () => { salahIndex = 0; },
    newChestState: () => ({ chests: {}, collection: [], shields: 0, circle: null })
  };
  window.Bayan = BAYAN;
})();
