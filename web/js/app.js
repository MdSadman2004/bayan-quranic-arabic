/* Bayan web client — views, lesson runtime, gamification surface. */
(function () {
  "use strict";
  const E = window.Bayan.E, A = window.Bayan.audio, CUR = window.Bayan.CUR;
  const STR = window.Bayan.STR, TAGLINE = window.Bayan.TAGLINE;
  const F = window.Bayan.F, L = window.Bayan.Loaders;
  const $ = (s, r) => (r || document).querySelector(s);

  let st = E.load();
  let view = { name: st.onboarded ? "home" : "onboarding", langPref: st.lang || "en" };
  let run = null;
  let lastResult = null;
  let toastTimer = null;
  let previousFocus = null;

  const t = (key, vars) => STR(key, st.lang, vars);
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
  const todayKey = () => E.dayKey(Date.now());
  const dailyXp = () => (st.day === todayKey() ? st.dailyXp : 0);

  /* ================================================================ icons */
  /* Bayan's own glyph family, drawn from the same 24x24 geometry as the
     Android client (Glyphs.kt): a khatim star for the streak, a dīnār seal for
     gems, a stair path, an echo loop, a mihrab arch, a ḥalaqah ring and a qalam
     nib. Deliberately not stock fire/diamond/people marks. */
  const G = (body, mark) => '<svg viewBox="0 0 24 24" class="g" data-mark="' + (mark || "") + '" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + body + "</svg>";
  const GLYPH = {
    star: G('<rect x="5.4" y="5.4" width="13.2" height="13.2" rx="1.2"/><rect x="5.4" y="5.4" width="13.2" height="13.2" rx="1.2" transform="rotate(45 12 12)"/><circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none"/>'),
    seal: G('<circle cx="12" cy="12" r="8.4"/><path d="M12 7.6 16.4 12 12 16.4 7.6 12z"/><path d="M6.6 12h10.8"/>', "seal"),
    path: '<svg viewBox="0 0 24 24" class="g" aria-hidden="true"><rect x="3.6" y="16.4" width="4.8" height="4.6" rx="1.4" fill="currentColor" opacity=".55"/><rect x="9.6" y="11.6" width="4.8" height="9.4" rx="1.4" fill="currentColor" opacity=".7"/><rect x="15.6" y="6.8" width="4.8" height="14.2" rx="1.4" fill="currentColor" opacity=".85"/></svg>',
    echo: G('<path d="M12 4.4a7.6 7.6 0 1 0 7.6 7.6"/><circle cx="18.4" cy="6.2" r="1.9" fill="currentColor" stroke="none"/>'),
    mihrab: G('<path d="M6.2 20.4V11.6c0-4 2.8-7.2 5.8-8.2 3 1 5.8 4.2 5.8 8.2v8.8"/><circle cx="12" cy="13.4" r="1.7" fill="currentColor" stroke="none"/>'),
    ring: G('<circle cx="12" cy="12" r="8.2"/><circle cx="12" cy="8.4" r="1.5" fill="currentColor" stroke="none"/><circle cx="8.9" cy="13.8" r="1.5" fill="currentColor" stroke="none"/><circle cx="15.1" cy="13.8" r="1.5" fill="currentColor" stroke="none"/>'),
    sprout: G('<path d="M12 21v-8"/><path d="M12 13c0-3.2 2.4-5.4 5.6-5.4 0 3.2-2.4 5.4-5.6 5.4z"/><path d="M12 15.2c0-2.6-2-4.4-4.6-4.4 0 2.6 2 4.4 4.6 4.4z"/>'),
    spark: G('<path d="M13.4 2.6 6.2 13.4h4.2l-1.4 8 7.4-11h-4.4z"/>'),
    lamp: G('<path d="M12 3.4c-3.4 0-6.2 2.8-6.2 6.2 0 2.6 1.7 4.9 4 5.7v2.4h4.4v-2.4c2.3-.8 4-3.1 4-5.7 0-3.4-2.8-6.2-6.2-6.2z"/><circle cx="12" cy="9.6" r="2.4" fill="currentColor" stroke="none"/><path d="M9.8 20.4h4.4"/>'),
    scroll: G('<rect x="4.6" y="4.2" width="14.8" height="15.6" rx="2.2"/><path d="M8.4 9h7.2M8.4 12.4h7.2M8.4 15.8h4.4"/>'),
    nib: G('<path d="M12 3.2 16.4 12 12 20.8 7.6 12z"/><path d="M12 9.4v6"/>')
  };
  /* Each achievement gets one of those marks instead of an emoji. */
  const ACH_GLYPH = {
    first_lesson: "sprout", lessons_10: "path", streak_3: "star", streak_7: "star",
    xp_100: "spark", xp_500: "spark", perfect: "seal", unit1: "mihrab",
    words_25: "sprout", words_50: "ring", review_1: "echo", halfway: "lamp"
  };
  const achMark = (a) => GLYPH[ACH_GLYPH[a && a.id] || "star"];

  const ICON = {
    heart: '<svg viewBox="0 0 24 24" class="i"><path d="M12 21C5 15.5 2 12.3 2 8.6 2 5.9 4 4 6.6 4c1.9 0 3.6 1 4.4 2.7C11.8 5 13.5 4 15.4 4 18 4 20 5.9 20 8.6c0 3.7-3 6.9-8 12.4z"/></svg>',
    lock: '<svg viewBox="0 0 24 24" class="i"><path d="M7 10V8a5 5 0 0 1 10 0v2h1a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1h1zm2 0h6V8a3 3 0 0 0-6 0v2z"/></svg>',
    check: '<svg viewBox="0 0 24 24" class="i"><path d="M9.5 16.2 5.3 12l-1.4 1.4 5.6 5.6 12-12-1.4-1.4z"/></svg>',
    speaker: '<svg viewBox="0 0 24 24" class="i"><path d="M4 9v6h4l5 4V5L8 9H4zm12.5 3a4.5 4.5 0 0 0-2.5-4v8a4.5 4.5 0 0 0 2.5-4z"/></svg>'
  };
  const LAMP = '<svg viewBox="0 0 64 64" class="lamp"><path d="M32 8c-9.4 0-17 7.6-17 17 0 7.3 4.6 13.5 11.1 15.9L25 54h14l-1.1-13.1C44.4 38.5 49 32.3 49 25c0-9.4-7.6-17-17-17z" fill="#C2542B"/><circle cx="32" cy="25" r="7.5" fill="#F7F2E8"/><circle cx="32" cy="25" r="3.2" fill="#26231D"/><path d="M26 54h12l-1.5 6h-9z" fill="#26231D"/></svg>';

  /* Tactile press: one click and one tiny vibration per press, anywhere in the
     app, gated by the sound setting. Registered once, in the capture phase so
     it runs before a handler can re-render or navigate. */
  function installPressFeedback() {
    const interactive = "button, [data-nav], [data-fui], [data-left], [data-right], [data-pick], [data-ret], [data-theme-set], .opt, .mleft, .mright, .tap, .clickable";
    document.addEventListener("pointerdown", (event) => {
      if (!st.sound) return;
      const node = event.target && event.target.closest ? event.target.closest(interactive) : null;
      if (!node) return;
      A.beep("tap");
      if (navigator.vibrate) { try { navigator.vibrate(8); } catch (ignored) { /* best effort */ } }
    }, true);
  }

  /* ================================================================ shell */
  function renderTabs() {
    const tabs = $("#tabs");
    const show = ["home", "practice", "rites", "circle", "profile"].indexOf(view.name) >= 0;
    tabs.className = "tabs" + (show ? "" : " hidden");
    if (!show) { tabs.innerHTML = ""; return; }
    const item = (name, label, icon) =>
      `<button class="tab ${view.name === name ? "active" : ""}" data-nav="${name}">${icon}<span>${esc(label)}</span></button>`;
    tabs.innerHTML =
      item("home", t("nav.learn"), GLYPH.path) +
      item("practice", t("nav.practice"), GLYPH.echo) +
      item("rites", t("nav.rites"), GLYPH.mihrab) +
      item("circle", t("nav.circle"), GLYPH.ring) +
      item("profile", t("nav.profile"), GLYPH.nib);
    tabs.querySelectorAll("[data-nav]").forEach(b => b.addEventListener("click", () => go(b.dataset.nav)));
  }
  function renderTopbar() {
    const bar = $("#topbar");
    const show = ["home", "practice", "profile"].indexOf(view.name) >= 0;
    bar.className = "topbar" + (show ? "" : " hidden");
    if (!show) { bar.innerHTML = ""; return; }
    bar.innerHTML =
      `<div class="chip streak" title="${esc(t("profile.streak"))}">${GLYPH.star}<b>${E.activeStreak(st)}</b></div>` +
      `<div class="chip" title="${esc(t("profile.gems"))}">${GLYPH.seal}<b>${st.gems}</b></div>` +
      `<div class="chip"><span class="xp">XP</span><b>${st.xp}</b></div>`;
  }
  function toast(msg) {
    const el = $("#toast");
    el.textContent = msg;
    el.className = "toast";
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.className = "toast hidden"; }, 2000);
  }
  function overlay(html) {
    const el = $("#overlay");
    previousFocus = document.activeElement;
    el.innerHTML = `<div class="overlay-inner" role="dialog" aria-modal="true" aria-label="${esc(t("common.continue"))}">${html}</div>`;
    el.className = "overlay";
    const first = el.querySelector("button");
    if (first) first.focus();
  }
  function closeOverlay() {
    $("#overlay").className = "overlay hidden"; $("#overlay").innerHTML = "";
    if (previousFocus && previousFocus.isConnected) previousFocus.focus();
  }
  function persist() {
    const ok = E.save(st);
    if (!ok) toast(t("backup.failed"));
    return ok;
  }

  /* -------------------------------------------------- feature bridge (FUI) */
  const bayanApi = {
    lang: () => st.lang,
    state: () => st,
    render: () => render(),
    go: (name) => go(name),
    toast: (message) => toast(message),
    save: () => persist(),
    canListen: () => A.canListen(),
    speak: (text) => { if (text) A.speak(text); },
    currentView: () => view.name,
    startPlacement: () => startPlacement(),
    answerPlacement: (index) => answerPlacement(index),
    advancePlacement: () => advancePlacement(),
    applyPlacement: () => applyPlacementNow(),
    undoPlacement: () => undoPlacementNow(),
    hasPlacementSession: () => !!placementSession,
    currentCard: () => cur(),
    record: (id, ok) => {
      if (!run) return;
      run.total += 1;
      if (ok) { run.correct += 1; touch(id, true); } else { run.mistakes += 1; touch(id, false); }
    },
    award: (xp, shields, source) => {
      if (xp > 0) st.xp += xp;
      if (shields > 0) st.shields = (st.shields || 0) + shields;
      if (st.day !== todayKey()) { st.day = todayKey(); st.dailyXp = 0; }
      st.dailyXp += xp || 0;
      persist();
    },
    openVerse: (verse) => {
      overlay(`<div class="ar ar-verse" dir="rtl" lang="ar">${esc(verse.ar)}</div>
        <p class="muted">${esc(verse[st.lang] || verse.en)}</p>
        <div class="ref">${esc(verse.ref)}</div>
        ${A.canListen() ? `<button class="btn btn-ghost btn-sm" id="ov-say">${esc(t("lesson.play"))}</button>` : ""}
        <div class="row"><button class="btn btn-ink" id="ov-no">${esc(t("common.continue"))}</button></div>`);
      const say = $("#ov-say");
      if (say) say.addEventListener("click", () => A.speak(verse.ar));
      $("#ov-no").addEventListener("click", closeOverlay);
    }
  };
  function viewRites() { return window.Bayan.FUI.ritesView(bayanApi); }
  function viewCircle() { return window.Bayan.FUI.circleView(bayanApi, st); }
  function viewBoss() { return window.Bayan.FUI.bossView(bayanApi); }

  /* ------------------------------------------------------- placement test */
  let placementSession = null;
  function viewPlacement() {
    return '<div class="pane">' + window.Bayan.FUI.placementView(bayanApi, placementSession) + "</div>";
  }
  function startPlacement() {
    placementSession = F.placementStart(E.CUR, st);
    F.placementQuestion(placementSession, E.CUR, st);
    go("placement");
  }
  function answerPlacement(index) {
    const q = placementSession && placementSession.current;
    if (!q || placementSession.graded) return;
    const ok = index === q.answer;
    placementSession.graded = { ok: ok, picked: index };
    if (st.sound) A.beep(ok ? "ok" : "bad");
    render();
  }
  function advancePlacement() {
    if (!placementSession) return;
    const ok = !!(placementSession.graded && placementSession.graded.ok);
    placementSession.graded = null;
    F.placementAnswer(placementSession, ok, E.CUR, st);
    if (placementSession.done) placementSession.result = F.placementResult(placementSession);
    else F.placementQuestion(placementSession, E.CUR, st);
    render();
  }
  function applyPlacementNow() {
    const tier = (placementSession && placementSession.result ? placementSession.result.tier : 0);
    const out = F.applyPlacement(st, tier, E.CUR);
    persist();
    placementSession = null;
    toast(t("placement.applied"));
    go("home");
    render();
  }
  function undoPlacementNow() {
    F.undoPlacement(st, E.CUR);
    persist();
    placementSession = null;
    toast(t("placement.undone"));
    go("home");
    render();
  }

  function render() {
    document.documentElement.lang = st.lang;
    document.documentElement.dataset.theme = st.theme || "auto";
    document.body.className = (st.lang === "bn" ? "lang-bn " : "") + (F && F.skyClass ? F.skyClass(new Date()) : "");
    const screen = $("#screen");
    if (view.name === "onboarding") screen.innerHTML = viewOnboarding();
    else if (view.name === "home") screen.innerHTML = viewHome();
    else if (view.name === "practice") screen.innerHTML = viewPractice();
    else if (view.name === "rites") screen.innerHTML = viewRites();
    else if (view.name === "circle") screen.innerHTML = viewCircle();
    else if (view.name === "boss") screen.innerHTML = viewBoss();
    else if (view.name === "placement") screen.innerHTML = viewPlacement();
    else if (view.name === "profile") screen.innerHTML = viewProfile();
    else if (view.name === "lesson") screen.innerHTML = viewLesson();
    bind();
    renderTabs();
    renderTopbar();
    const error = window.Bayan.Store.error;
    let warning = $("#storage-warning");
    if (!warning) { warning = document.createElement("p"); warning.id = "storage-warning"; warning.className = "storage-warning"; warning.setAttribute("role", "alert"); $("#app").prepend(warning); }
    warning.textContent = error ? t("backup.failed") : "";
    warning.hidden = !error;
    window.scrollTo(0, 0);
  }
  function go(name) {
    if (run && name !== "lesson") run = null;
    view = Object.assign({}, view, { name: name });
    render();
  }

  /* ================================================================ onboarding */
  function viewOnboarding() {
    const l = view.langPref;
    return `<div class="onb">
      ${LAMP}
      <h1 class="display">Bayan</h1>
      <p class="onb-tag">${esc(TAGLINE[l] || TAGLINE.en)}</p>
      <ul class="onb-points">
        <li>${esc(STR("onb.p1", l))}</li>
        <li>${esc(STR("onb.p2", l))}</li>
        <li>${esc(STR("onb.p3", l))}</li>
      </ul>
      <div class="onb-choose">${esc(STR("onb.choose", l))}</div>
      <div class="lang-row">
        <button class="lang-btn ${l === "en" ? "sel" : ""}" data-lang="en">English</button>
        <button class="lang-btn ${l === "bn" ? "sel" : ""}" data-lang="bn">বাংলা</button>
      </div>
      <button class="btn btn-accent btn-block" id="onb-start">${esc(STR("onb.start", l))}</button>
    </div>`;
  }

  /* ================================================================ home */
  function viewHome() {
    const goal = st.dailyGoal || 30;
    const dx = dailyXp();
    const pct = Math.min(1, dx / goal);
    const R = 26, C = 2 * Math.PI * R;
    const q = E.reviewQueue(st, Date.now());
    const due = E.dueIds(st, Date.now()).length;
    const seen = E.seenCount(st);
    const reviewSub = seen === 0 ? t("practice.emptyHint")
      : due > 0 ? t("home.due", { n: due }) : t("home.allDone");
    const nextId = E.nextLessonId(st);

    let html = `<div class="home">
      ${F ? window.Bayan.FUI.placementBanner(bayanApi, st) : ""}
      <div class="goal-card">
        <button class="ring-wrap" id="goal-ring" aria-label="${esc(t("goal.change"))}">
          <svg viewBox="0 0 64 64" class="ring">
            <circle cx="32" cy="32" r="${R}" class="ring-bg"/>
            <circle cx="32" cy="32" r="${R}" class="ring-fg" stroke-dasharray="${C.toFixed(1)}" stroke-dashoffset="${(C * (1 - pct)).toFixed(1)}"/>
          </svg>
          <div class="ring-txt"><b>${dx}</b><span>/${goal}</span></div>
        </button>
        <div class="goal-info">
          <div class="goal-title">${esc(t("home.goal"))}</div>
          <div class="goal-sub">${esc(t("home.goalXp", { n: dx }))}</div>
        </div>
      </div>
      <button class="review-card ${seen === 0 ? "dim" : ""}" id="go-review">
        <span class="rev-ico">${GLYPH.echo}</span>
        <span class="rev-body"><b>${esc(t("home.review"))}</b><small>${esc(reviewSub)}</small></span>
        ${due > 0 ? `<span class="rev-badge">${due}</span>` : ""}
      </button>`;

    for (const u of E.UNITS) {
      const prog = E.unitProgress(st, u.id);
      const allDone = prog.done === prog.total;
      html += `<section class="unit ${allDone ? "unit-complete" : ""}">
        <header class="unit-head">
          <div class="unit-meta">
            <span class="unit-chip">${esc(t("home.unit", { n: u.num }))}</span>
            <h2>${esc(u.title[st.lang] || u.title.en)}</h2>
            <p>${esc(u.subtitle[st.lang] || u.subtitle.en)}</p>
          </div>
          <div class="unit-prog">
            <b>${prog.done}/${prog.total}</b>
            ${allDone ? ICON.check : ""}
          </div>
        </header>
        <div class="path">`;
      u.lessons.forEach((lid, i) => {
        const done = E.lessonDone(st, lid);
        const unlocked = E.isUnlocked(st, lid);
        const current = unlocked && !done && lid === nextId;
        const stars = (st.lessons[lid] && st.lessons[lid].stars) || 0;
        const cls = done ? "done" : !unlocked ? "locked" : current ? "current" : "open";
        html += `<div class="node-row ${i % 2 ? "alt" : ""}">
          <button class="node ${cls}" data-lesson="${lid}" ${unlocked ? "" : "disabled"}>
            <span class="node-face">${done ? ICON.check : unlocked ? GLYPH.path : ICON.lock}</span>
            ${current ? `<span class="bubble">${esc(t("home.start"))}</span>` : ""}
          </button>
          <span class="node-label">${esc(E.lessonObject(lid).title[st.lang] || E.lessonObject(lid).title.en)}</span>
          ${done ? `<span class="stars">${"★".repeat(stars)}${"☆".repeat(3 - stars)}</span>` : ""}
        </div>`;
      });
      html += `</div></section>`;
    }
    if (F && window.Bayan.FUI) html += window.Bayan.FUI.homeExtras(bayanApi, st);
    html += `</div>`;
    return html;
  }

  /* ================================================================ practice */
  function viewPractice() {
    const due = E.dueIds(st, Date.now()).length;
    const seen = E.seenCount(st);
    const mistakes = E.mistakeIds(st).length;
    return `<div class="pane">
      <h1 class="display">${esc(t("practice.title"))}</h1>
      <div class="review-big">
        <p class="big-ico">${GLYPH.echo}</p>
        <h3>${esc(seen === 0 ? t("practice.empty") : due > 0 ? t("practice.due", { n: due }) : t("home.practiceWeak"))}</h3>
        <p class="muted">${esc(t(seen === 0 ? "practice.emptyHint" : "practice.note"))}</p>
        ${seen ? `<button class="btn btn-accent btn-block" id="start-review">${esc(t("practice.start"))}</button>` : ""}
        ${mistakes ? `<button class="btn btn-ghost btn-block" id="review-mistakes">${esc(t("library.review"))} (${mistakes})</button>` : ""}
      </div>
      ${F ? window.Bayan.FUI.practiceExtras(bayanApi, st) : ""}
      <section class="library">
        <h2 class="sect">${esc(t("library.title"))}</h2>
        <p class="muted">${esc(t("library.note"))}</p>
        <label class="search-label" for="word-search">${esc(t("library.search"))}</label>
        <input id="word-search" type="search" maxlength="100" value="${esc(view.search || "")}" autocomplete="off">
        <div class="library-filters" role="group" aria-label="${esc(t("library.title"))}">
          ${["all", "seen", "mistakes"].map(f => `<button class="seg-btn ${(view.filter || "all") === f ? "sel" : ""}" data-filter="${f}" aria-pressed="${(view.filter || "all") === f}">${esc(t("library." + f))}</button>`).join("")}
        </div>
        <div id="word-results">${wordResults()}</div>
      </section>
    </div>`;
  }
  function wordResults() {
    const items = E.searchItems(st, view.search, view.filter);
    return `<p class="muted word-count" role="status">${esc(t("library.count", { n: items.length }))}</p>` + (items.length ? items.map(it => {
      const r = st.items[it.id];
      const status = !r || !r.seen ? t("library.new") : r.dueAt <= Date.now() ? t("library.due") : t("library.level", { n: r.lvl });
      return `<article class="word-row"><div class="word-ar"><span class="ar" dir="rtl" lang="ar">${esc(it.ar)}</span>${it.root ? `<small class="ar" dir="rtl" lang="ar">${esc(it.root)}</small>` : ""}</div><div class="word-meaning"><b>${esc(E.meaning(it, st.lang))}</b><small>${esc(status)}</small></div></article>`;
    }).join("") : `<p class="muted empty-results">${esc(t("library.empty"))}</p>`);
  }

  /* ================================================================ profile */
  function viewProfile() {
    const s = st, stats = s.stats;
    const acc = stats.answered ? Math.round((stats.correct / stats.answered) * 100) : 0;
    const tiles = [
      [t("profile.xp"), s.xp], [t("profile.streak"), E.activeStreak(s)], [t("profile.gems"), s.gems],
      [t("profile.words"), E.learnedCount(s)], [t("profile.mastered"), E.masteredCount(s)],
      [t("profile.accuracy"), acc + "%"], [t("profile.lessons"), stats.lessons], [t("profile.perfects"), stats.perfects]
    ];
    const cov = E.coverage(s).toFixed(1);
    let html = `<div class="pane">
      <h1 class="display">${esc(t("profile.title"))}</h1>
      <div class="stat-grid">${tiles.map(([k, v]) => `<div class="stat"><b>${v}</b><span>${esc(k)}</span></div>`).join("")}</div>
      <div class="coverage">
        <div class="cov-bar"><div class="cov-fill" style="width:${Math.min(100, cov)}%"></div></div>
        <p class="muted">${esc(t("profile.coverage", { p: cov }))}</p>
      </div>
      <h2 class="sect">${esc(t("profile.achievements"))}</h2>
      <div class="ach-grid">${E.ACHIEVEMENTS.map(a => {
        const got = !!s.achievements[a.id];
        return `<div class="ach ${got ? "got" : "locked"}" title="${esc(a.desc[st.lang] || a.desc.en)}">
          <span class="ach-ico">${achMark(a)}</span>
          <b>${esc(a.name[st.lang] || a.name.en)}</b>
          <small>${esc(a.desc[st.lang] || a.desc.en)}</small>
        </div>`;
      }).join("")}</div>
      <h2 class="sect">${esc(t("profile.settings"))}</h2>
      <div class="settings">
        <div class="set-row"><span>${esc(t("profile.language"))}</span>
          <div class="seg">
            <button class="seg-btn ${st.lang === "en" ? "sel" : ""}" data-setlang="en">English</button>
            <button class="seg-btn ${st.lang === "bn" ? "sel" : ""}" data-setlang="bn">বাংলা</button>
          </div>
        </div>
        <div class="set-row"><span>${esc(t("profile.sound"))}</span>
          <button class="toggle ${st.sound ? "on" : ""}" id="tg-sound" role="switch" aria-checked="${st.sound}" aria-label="${esc(t("profile.sound"))}"><span></span></button>
        </div>
        <div class="set-row"><span>${esc(t("profile.about"))}</span></div>
        <p class="muted small">${A.canListen() ? esc(t("profile.ttsOn")) : esc(t("profile.ttsOff"))}</p>
        <p class="muted small">${esc(t("profile.aboutText"))}</p>
        <div class="set-row"><span></span><button class="btn btn-ghost" id="reset-progress">${esc(t("profile.reset"))}</button></div>
      </div>
      <h2 class="sect">${esc(t("profile.theme"))}</h2>
      <div class="settings">
        <p class="muted small">${esc(t("profile.themeNote"))}</p>
        <div class="theme-row">
          ${["auto", "light", "dark"].map(mode => `<button class="btn btn-ghost theme-choice ${(st.theme || "auto") === mode ? "btn-ink" : ""}" data-theme-set="${mode}">${esc(t("theme." + mode))}</button>`).join("")}
        </div>
      </div>
      <h2 class="sect">${esc(t("backup.title"))}</h2>
      <div class="settings backup-settings">
        <p class="muted">${esc(t("backup.note"))}</p>
        <div class="backup-actions"><button class="btn btn-ghost" id="export-backup">${esc(t("backup.export"))}</button><button class="btn btn-ghost" id="import-backup">${esc(t("backup.import"))}</button></div>
        <input type="file" id="backup-file" accept="application/json,.json" class="hidden">
        <p class="offline-status" role="status">${esc(t(window.Bayan.offline && window.Bayan.offline.ready ? "offline.ready" : "offline.pending"))}</p>
        <p class="muted small">${esc(t("offline.note"))}</p>
      </div>
    </div>`;
    return html;
  }

  /* ================================================================ lesson */
  function cur() { return run ? run.cards[run.i] : null; }
  function startLesson(id) {
    if (!E.isUnlocked(st, id)) { toast(t("home.locked")); return; }
    const lesson = E.lessonObject(id);
    run = {
      mode: "lesson", lessonId: id,
      cards: E.compileLesson(id, st, { tts: () => A.canListen() }),
      i: 0, hearts: E.MAX_HEARTS, combo: 0, maxCombo: 0, correct: 0, total: 0, mistakes: 0,
      touches: {}, startedAt: Date.now(), phase: "answer", selected: null, graded: null,
      buildSel: [], match: { done: [], selLeft: null, flash: null }, failed: false, finalized: false
    };
    run.flow = { streak: 0, lastAt: 0, multiplier: 1, semitones: 0 };
    if (F && window.Bayan.FUI) {
      window.Bayan.FUI.injectForge(run.cards, lesson, E.rng("forge:" + id + ":" + Date.now()));
      const screen = $("#screen");
      if (screen && lesson) L.show(screen, window.Bayan.FUI.lessonLoader(lesson), 620);
    }
    go("lesson");
  }
  function startReview(ids) {
    const q = Array.isArray(ids) ? ids.slice(0, E.REVIEW_SIZE) : E.reviewQueue(st, Date.now());
    if (q.length === 0) { toast(t("practice.empty")); return; }
    run = {
      mode: "review", lessonId: null, reviewIds: q,
      cards: E.compileReview(st, q, { tts: () => A.canListen() }),
      i: 0, hearts: E.MAX_HEARTS, combo: 0, maxCombo: 0, correct: 0, total: 0, mistakes: 0,
      touches: {}, startedAt: Date.now(), phase: "answer", selected: null, graded: null,
      buildSel: [], match: { done: [], selLeft: null, flash: null }, failed: false, finalized: false
    };
    go("lesson");
  }

  function touch(id, ok) {
    if (!id) return;
    const r = run.touches[id] = run.touches[id] || { ok: 0, bad: 0 };
    if (ok) r.ok++; else r.bad++;
  }
  function recordAnswer(id, ok, isMatchTap) {
    run.total++;
    if (ok) {
      run.correct++; run.combo++; run.maxCombo = Math.max(run.maxCombo, run.combo);
      touch(id, true);
    } else {
      run.mistakes++; run.combo = 0; run.hearts--;
      touch(id, false);
      if (!isMatchTap) run.graded = run.graded || {};
      if (run.hearts <= 0) run.failed = true;
    }
    if (F && F.comboTick) run.flow = F.comboTick(run.flow, ok, Date.now());
    if (st.sound) A.beep(ok ? "ok" : "bad", run.flow ? run.flow.semitones : 0);
  }

  function gradeChoice(idx) {
    const card = cur();
    if (!run || run.failed || run.phase !== "answer" || !card.options || !Number.isInteger(idx) || idx < 0 || idx >= card.options.length) return;
    run.selected = idx;
    render();
  }
  function doCheck() {
    const card = cur();
    if (!card || run.failed || run.phase !== "answer") return;
    if (card.t === "build") {
      if (!run.buildSel.length) return;
      const joined = run.buildSel.map(i => card.tiles[i]).join(" ");
      const ok = joined === card.answer;
      recordAnswer(card.id, ok);
      run.graded = { ok: ok, correctText: card.answer };
      run.phase = "feedback";
      render();
      return;
    }
    if (run.selected == null) return;
    const ok = E.check(card, run.selected);
    recordAnswer(card.id, ok);
    run.graded = { ok: ok, correctText: card.options[card.answer] };
    run.phase = "feedback";
    render();
  }
  function doContinue() {
    if (!run || run.failed) return;
    const card = cur();
    if (!card) return;
    if (card.t === "complete") { run = null; go("home"); return; }
    const forgeGraded = card.t === "forge" && card.graded;
    if (!["teach", "context", "read"].includes(card.t) && !forgeGraded && run.phase !== "feedback") return;
    run.phase = "answer"; run.selected = null; run.graded = null; run.buildSel = [];
    run.match = { done: [], selLeft: null, flash: null };
    run.i++;
    if (run.cards[run.i] && run.cards[run.i].t === "complete") finalize();
    render();
  }
  function finalize() {
    if (run.finalized) return;
    run.finalized = true;
    run.secs = Math.round((Date.now() - run.startedAt) / 1000);
    const rootsBefore = F ? F.knownRoots(st) : new Set();
    lastResult = E.applyLessonResult(st, run);
    if (F) {
      const flowMult = F.flowMultiplier(run.maxCombo);
      const flowTotal = F.flowXp(lastResult.xp, run.maxCombo);
      const flowExtra = flowTotal - lastResult.xp;
      st.xp += flowExtra;
      lastResult.xp = flowTotal;
      lastResult.flow = flowMult;
      if (st.day !== todayKey()) { st.day = todayKey(); st.dailyXp = 0; }
      st.dailyXp += flowExtra;
      const chest = F.rollChest(run, Math.random);
      if (chest) { F.applyLoot(st, chest); lastResult.chest = chest; }
      const fresh = [];
      F.knownRoots(st).forEach((root) => { if (!rootsBefore.has(root)) fresh.push(root); });
      lastResult.seeds = fresh.length ? F.seedsForRoots(fresh) : [];
      st.circle = F.circleTick(st.circle || F.newCircle(), true);
    }
    persist();
    if (st.sound) A.beep("complete");
  }

  function cardTag(text, kind) { return `<div class="card-tag ${kind || ""}">${esc(text)}</div>`; }

  function renderCard() {
    const card = cur();
    if (!card) return "";
    switch (card.t) {
      case "teach": {
        const tt = card.teach;
        const kindLabel = tt.kind === "concept" ? t("lesson.concept") : t("lesson.newWord");
        const note = tt.note ? `<div class="note">${esc(tt.note[st.lang] || tt.note.en)}</div>` : "";
        return `<div class="card card-teach">
          ${cardTag(kindLabel, tt.kind)}
          <div class="ar ar-xl" dir="rtl" lang="ar">${esc(tt.ar)}</div>
          <div class="meaning">${esc(tt[st.lang] || tt.en)}</div>
          ${note}
        </div>`;
      }
      case "context": {
        const v = card.verse, cx = v.context;
        const kindMap = { asbab: "asbab", story: "story", theme: "theme", others: "others" };
        return `<div class="card card-context">
          ${cardTag(t("lesson.context"), kindMap[cx.kind] || "")}
          <h3 class="ctx-title">${esc(cx.title[st.lang] || cx.title.en)}</h3>
          <p class="ctx-body">${esc(cx.body[st.lang] || cx.body.en)}</p>
          <div class="ref">${esc(v.ref)}</div>
        </div>`;
      }
      case "read": {
        const v = card.verse;
        const revealed = run.readRevealed && run.readRevealed[card.id];
        return `<div class="card card-read">
          ${cardTag(t("lesson.quran"))}
          <div class="ar ar-verse" dir="rtl" lang="ar">${esc(v.ar)}</div>
          ${A.canListen() ? `<button class="btn btn-ghost btn-sm" id="play-verse">${ICON.speaker} ${esc(t("lesson.play"))}</button>` : ""}
          <div class="translation ${revealed ? "" : "hidden"}">
            <p>${esc(v[st.lang] || v.en)}</p>
            <div class="ref">${esc(v.ref)}</div>
          </div>
          ${revealed ? "" : `<button class="reveal" id="reveal">${esc(t("lesson.reveal"))}</button>`}
        </div>`;
      }
      case "mc": case "mca": case "root": case "listen": case "blank": {
        const isArOptions = card.t !== "mc";
        const q = card.t === "mc" ? t("lesson.whatMean")
          : card.t === "mca" ? t("lesson.chooseAr")
          : card.t === "root" ? t("lesson.rootQ")
          : card.t === "listen" ? t("lesson.listenQ")
          : t("lesson.blankQ");
        let body = "";
        if (card.t === "listen") {
          body = `<div class="listen-box"><button class="btn btn-accent btn-block" id="play-listen">${ICON.speaker} ${esc(t("lesson.play"))}</button></div>`;
        } else if (card.t === "blank") {
          const masked = card.verse.tokens.map((tk, i) => i === card.blank ? '<span class="blank-slot">____</span>' : esc(tk)).join(" ");
          body = `<div class="ar ar-verse" dir="rtl" lang="ar">${masked}</div>
                  <div class="blank-hint">${esc(card.verse[st.lang] || card.verse.en)}</div>`;
        } else {
          const promptCls = card.t === "mc" ? "ar ar-xl" : "ar ar-lg";
          const promptDir = card.t === "mc" ? 'dir="rtl" lang="ar"' : "";
          body = `<div class="${promptCls}" ${promptDir}>${esc(card.prompt)}</div>`;
        }
        const opts = card.options.map((o, i) => {
          const cls = ["opt"];
          if (isArOptions) cls.push("opt-ar");
          if (run.phase === "feedback") {
            if (i === card.answer) cls.push("correct");
            else if (i === run.selected) cls.push("wrong");
            cls.push("frozen");
          } else if (i === run.selected) cls.push("sel");
          return `<button class="${cls.join(" ")}" data-i="${i}" ${isArOptions ? 'dir="rtl" lang="ar"' : ""} ${run.phase !== "answer" ? "disabled" : ""}>${esc(o)}</button>`;
        }).join("");
        return `<div class="card card-q">
          <div class="q-label">${esc(q)}</div>
          ${body}
          <div class="opts ${card.options.some(o => o.length > 14) ? "col" : "grid2"}">${opts}</div>
        </div>`;
      }
      case "build": {
        const v = card.verse;
        const selSet = new Set(run.buildSel);
        const answerRow = run.buildSel.map(i => `<button class="tile sel-tile" data-ret="${i}" dir="rtl" lang="ar">${esc(card.tiles[i])}</button>`).join("");
        const pool = card.tiles.map((tk, i) => selSet.has(i) ? "" : `<button class="tile" data-pick="${i}" dir="rtl" lang="ar">${esc(tk)}</button>`).join("");
        const graded = run.phase === "feedback";
        return `<div class="card card-build">
          <div class="q-label">${esc(t("lesson.buildQ"))}</div>
          <div class="build-prompt">${esc(v[st.lang] || v.en)}</div>
          <div class="build-area ${graded ? (run.graded.ok ? "ok" : "bad") : ""}" dir="rtl" lang="ar">${answerRow}</div>
          ${graded && !run.graded.ok ? `<div class="ar ar-lg" dir="rtl" lang="ar">${esc(run.graded.correctText)}</div>` : ""}
          <div class="tiles" dir="rtl" lang="ar">${pool}</div>
        </div>`;
      }
      case "match": {
        const done = new Set(run.match.done);
        const left = card.pairs.map(p => `<button class="mleft ${done.has(p.id) ? "matched" : run.match.selLeft === p.id ? "sel" : ""}" data-left="${p.id}" dir="rtl" lang="ar" ${done.has(p.id) ? "disabled" : ""}>${esc(p.left)}</button>`).join("");
        const byId = {};
        card.pairs.forEach(p => (byId[p.id] = p));
        const right = card.rights.map(id => `<button class="mright ${done.has(id) ? "matched" : run.match.flash === id ? "flash" : ""}" data-right="${id}" ${done.has(id) ? "disabled" : ""}>${esc(byId[id].right)}</button>`).join("");
        return `<div class="card card-match">
          <div class="q-label">${esc(t("lesson.matchTitle"))}</div>
          <div class="match"><div class="mcol">${left}</div><div class="mcol">${right}</div></div>
        </div>`;
      }
      case "forge": {
        return window.Bayan.FUI.forgeCard(card, bayanApi);
      }
      case "complete": {
        const r = lastResult || { xp: 0, acc: 0, perfect: false, newAch: [] };
        const ach = (r.newAch || []).map(id => {
          const a = E.ACHIEVEMENTS.find(x => x.id === id);
          return a ? `<div class="ach-toast"><span class="ach-ico">${achMark(a)}</span> <b>${esc(a.name[st.lang] || a.name.en)}</b> — ${esc(a.desc[st.lang] || a.desc.en)}</div>` : "";
        }).join("");
        const extras = F ? window.Bayan.FUI.completedExtras(bayanApi, st, r) : "";
        return `<div class="card card-complete">
          <div class="done-badge">${GLYPH.star}</div>
          <h2 class="display">${r.perfect ? esc(t("lesson.perfect")) : esc(t("lesson.again"))}</h2>
          <div class="res-grid">
            <div class="res"><b>+${r.xp}</b><span>${esc(t("lesson.xpEarned"))}</span></div>
            <div class="res"><b>${Math.round(r.acc * 100)}%</b><span>${esc(t("lesson.accuracy"))}</span></div>
            <div class="res"><b>${run.maxCombo}</b><span>${esc(t("lesson.combo"))}</span></div>
          </div>
          ${r.unitComplete ? `<div class="unit-done">${esc(t("lesson.unitDone"))}</div>` : ""}
          ${ach}
          ${extras}
        </div>`;
      }
      default: return "";
    }
  }

  function footHtml() {
    const card = cur();
    if (card && card.t === "forge") {
      if (card.graded) {
        return `<div class="foot feedback ${card.graded.ok ? "ok" : "bad"}">
          <div class="fb-msg">${esc(card.graded.ok ? t("lesson.correct") : (card.round.word + " — " + card.round.meaning[st.lang]))}</div>
          ${footAction("continue", t("lesson.continue"), false)}
        </div>`;
      }
      return `<div class="foot"><p class="muted match-instruction">${esc(t("forge.ask"))}</p></div>`;
    }
    if (card && card.t === "match" && run.phase !== "feedback") {
      return `<div class="foot"><p class="muted match-instruction">${esc(t("lesson.matchTitle"))}</p></div>`;
    }
    if (!card || card.t === "complete") {
      return `<div class="foot">${footAction("continue", t("lesson.continue"), false)}</div>`;
    }
    if (card.t === "teach" || card.t === "context" || card.t === "read" || card.t === "complete") {
      return `<div class="foot">${footAction("continue", t("lesson.continue"), false)}</div>`;
    }
    if (card.t === "match") return `<div class="foot">${footAction("continue", t("lesson.continue"), false)}</div>`;
    if (run.phase === "feedback") {
      const ok = run.graded.ok;
      const msg = ok ? t("lesson.correct") : t("lesson.wrong") + " " + run.graded.correctText;
      return `<div class="foot feedback ${ok ? "ok" : "bad"}">
        <div class="fb-msg">${esc(msg)}</div>
        ${footAction("continue", t("lesson.continue"), false)}
      </div>`;
    }
    let disabled = true, label = t("lesson.check");
    if (card.t === "build") disabled = run.buildSel.length === 0;
    else disabled = run.selected == null;
    return `<div class="foot">${footAction("check", label, disabled)}</div>`;
  }
  function footAction(id, label, disabled) {
    return `<button class="btn btn-block ${id === "check" ? "btn-ink" : "btn-accent"}" id="${id === "check" ? "do-check" : "do-continue"}" ${disabled ? "disabled" : ""}>${esc(label)}</button>`;
  }

  function viewLesson() {
    if (!run) return "";
    const card = cur();
    if (!card) return "";
    if (run.failed && card.t !== "complete") {
      return `<div class="lesson">
        <div class="hud"><button class="icon-btn" id="quit">✕</button><div class="progress"><div style="width:100%"></div></div><div class="hearts"></div></div>
        <div class="card card-fail">
          <div class="fail-ico">${ICON.heart}</div>
          <h2 class="display">${esc(t("lesson.outHearts"))}</h2>
          <p class="muted">${run.hearts <= 0 ? esc(t("lesson.tryAgain")) : ""}</p>
          <button class="btn btn-accent btn-block" id="retry">${esc(t("lesson.retry"))}</button>
          <button class="btn btn-ghost btn-block" id="quit2">${esc(t("lesson.quit"))}</button>
        </div>
      </div>`;
    }
    const total = run.cards.length - 1;
    const pct = Math.max(3, Math.round((run.i / total) * 100));
    let hearts = "";
    for (let i = 1; i <= E.MAX_HEARTS; i++) hearts += `<span class="h ${i <= run.hearts ? "full" : ""}">${ICON.heart}</span>`;
    return `<div class="lesson">
      <div class="hud">
        <button class="icon-btn" id="quit">✕</button>
        <div class="progress"><div style="width:${pct}%"></div></div>
        <div class="hearts">${hearts}</div>
      </div>
      <div class="card-wrap">${renderCard()}</div>
      ${F ? window.Bayan.FUI.flowChip(run) : `<div class="combo-chip ${run.combo >= 3 ? "show" : ""}">×${run.combo}</div>`}
      ${footHtml()}
    </div>`;
  }

  /* ================================================================ bindings */
  function bind() {
    if (window.Bayan.FUI) window.Bayan.FUI.bind(bayanApi);
    document.querySelectorAll("[data-theme-set]").forEach(b =>
      b.addEventListener("click", () => { st.theme = b.dataset.themeSet; persist(); render(); }));
    if (view.name === "onboarding") {
      document.querySelectorAll("[data-lang]").forEach(b =>
        b.addEventListener("click", () => { view.langPref = b.dataset.lang; render(); }));
      const start = $("#onb-start");
      if (start) start.addEventListener("click", () => {
        st.onboarded = true; st.lang = view.langPref;
        E.save(st); go("home");
      });
    }
    if (view.name === "home") {
      const g = $("#goal-ring");
      if (g) g.addEventListener("click", () => {
        const goals = [20, 30, 50, 100];
        st.dailyGoal = goals[(goals.indexOf(st.dailyGoal) + 1) % goals.length] || 30;
        E.save(st); render();
      });
      const rev = $("#go-review");
      if (rev) rev.addEventListener("click", () => {
        if (E.seenCount(st) === 0) { toast(t("practice.emptyHint")); return; }
        go("practice");
      });
      document.querySelectorAll("[data-lesson]").forEach(b =>
        b.addEventListener("click", () => {
          const lid = b.dataset.lesson;
          if (!E.isUnlocked(st, lid)) { toast(t("home.locked")); return; }
          startLesson(lid);
        }));
    }
    if (view.name === "practice") {
      const sr = $("#start-review");
      if (sr) sr.addEventListener("click", startReview);
    }
    if (view.name === "profile") {
      document.querySelectorAll("[data-setlang]").forEach(b =>
        b.addEventListener("click", () => { st.lang = b.dataset.setlang; E.save(st); render(); }));
      const tg = $("#tg-sound");
      if (tg) tg.addEventListener("click", () => { st.sound = !st.sound; E.save(st); render(); });
      const rs = $("#reset-progress");
      if (rs) rs.addEventListener("click", () => {
        overlay(`<p>${esc(t("profile.resetAsk"))}</p><div class="row">
          <button class="btn btn-ghost" id="ov-no">${esc(t("lesson.quitNo"))}</button>
          <button class="btn btn-danger" id="ov-yes">${esc(t("profile.reset"))}</button></div>`);
        $("#ov-no").addEventListener("click", closeOverlay);
        $("#ov-yes").addEventListener("click", () => {
          try { localStorage.removeItem("bayan.v1"); } catch (e) { /* ignore */ }
          st = E.newState(); closeOverlay(); go("onboarding");
        });
      });
    }
    if (view.name === "lesson") bindLesson();
    $("#overlay").addEventListener("click", e => { if (e.target.id === "overlay") closeOverlay(); });
  }

  function bindLesson() {
    const card = cur();
    const q = $("#quit"), q2 = $("#quit2");
    const quitAsk = () => {
      overlay(`<p>${esc(t("lesson.quitAsk"))}</p><div class="row">
        <button class="btn btn-ghost" id="ov-no">${esc(t("lesson.quitNo"))}</button>
        <button class="btn btn-ink" id="ov-yes">${esc(t("lesson.quitYes"))}</button></div>`);
      $("#ov-no").addEventListener("click", closeOverlay);
      $("#ov-yes").addEventListener("click", () => { closeOverlay(); run = null; go("home"); });
    };
    if (q) q.addEventListener("click", quitAsk);
    if (q2) q2.addEventListener("click", () => { run = null; go("home"); });
    const rt = $("#retry");
    if (rt) rt.addEventListener("click", () => {
      if (run.mode === "review") startReview(); else startLesson(run.lessonId);
    });
    if (!card) return;

    if (card.t === "teach" || card.t === "context") {
      const c = $("#do-continue");
      if (c) c.addEventListener("click", doContinue);
      return;
    }
    if (card.t === "read") {
      const rv = $("#reveal");
      if (rv) rv.addEventListener("click", () => {
        run.readRevealed = run.readRevealed || {};
        run.readRevealed[card.id] = true;
        render();
      });
      const pv = $("#play-verse");
      if (pv) pv.addEventListener("click", () => A.speak(card.verse.ar));
      const c = $("#do-continue");
      if (c) c.addEventListener("click", doContinue);
      return;
    }
    if (card.t === "mc" || card.t === "mca" || card.t === "root" || card.t === "listen" || card.t === "blank") {
      const pl = $("#play-listen");
      if (pl) pl.addEventListener("click", () => A.speak(card.ref.ar));
      if (card.t === "listen" && run.phase === "answer") A.speak(card.ref.ar);
      document.querySelectorAll(".opt").forEach(b =>
        b.addEventListener("click", () => { if (run.phase === "answer") gradeChoice(Number(b.dataset.i)); }));
      const c = $("#do-continue");
      if (c) c.addEventListener("click", doContinue);
      const ck = $("#do-check");
      if (ck) ck.addEventListener("click", doCheck);
      return;
    }
    if (card.t === "build") {
      document.querySelectorAll("[data-pick]").forEach(b =>
        b.addEventListener("click", () => {
          if (run.phase !== "answer") return;
          run.buildSel.push(Number(b.dataset.pick)); render();
        }));
      document.querySelectorAll("[data-ret]").forEach(b =>
        b.addEventListener("click", () => {
          if (run.phase !== "answer") return;
          const i = Number(b.dataset.ret);
          run.buildSel = run.buildSel.filter(x => x !== i); render();
        }));
      const ck = $("#do-check");
      if (ck) ck.addEventListener("click", doCheck);
      const c = $("#do-continue");
      if (c) c.addEventListener("click", doContinue);
      return;
    }
    if (card.t === "match") {
      document.querySelectorAll("[data-left]").forEach(b =>
        b.addEventListener("click", () => { run.match.selLeft = b.dataset.left; render(); }));
      document.querySelectorAll("[data-right]").forEach(b =>
        b.addEventListener("click", () => {
          const id = b.dataset.right;
          if (!run.match.selLeft) return;
          if (run.match.selLeft === id) {
            run.match.done.push(id);
            run.match.selLeft = null;
            recordAnswer(id, true, true);
            if (run.match.done.length === card.pairs.length) {
              run.graded = { ok: true, correctText: "" };
              run.phase = "feedback";
              render();
              setTimeout(() => { if (run && run.phase === "feedback") doContinue(); }, 650);
              return;
            }
          } else {
            recordAnswer(id, false, true);
            run.match.flash = id;
            run.combo = 0;
            setTimeout(() => { if (run) { run.match.flash = null; render(); } }, 380);
          }
          render();
        }));
      const c = $("#do-continue");
      if (c) c.addEventListener("click", doContinue);
      return;
    }
    if (card.t === "forge") {
      const c = $("#do-continue");
      if (c) c.addEventListener("click", doContinue);
      return;
    }
    if (card.t === "complete") {
      const c = $("#do-continue");
      if (c) c.addEventListener("click", () => { const doneLesson = run.lessonId; run = null; go("home"); });
    }
  }

  /* ================================================================ keyboard */
  document.addEventListener("keydown", e => {
    if (view.name !== "lesson" || !run) return;
    const card = cur();
    if (!card) return;
    if (e.key >= "1" && e.key <= "9") {
      const idx = Number(e.key) - 1;
      if ((card.t === "mc" || card.t === "mca" || card.t === "root" || card.t === "blank") && run.phase === "answer" && idx < card.options.length) {
        gradeChoice(idx);
      }
    } else if (e.key === "Enter") {
      if (run.phase === "feedback" || !["mc", "mca", "root", "listen", "blank", "build"].includes(card.t)) doContinue();
      else doCheck();
    }
  });

  /* ================================================================ dev hook */
  window.__bayan = {
    get st() { return st; },
    get run() { return run; },
    get view() { return view; },
    get placement() { return placementSession; },
    startPlacement,
    card: () => (run ? run.cards[run.i] : null),
    startLesson, startReview, go,
    select: i => { const c = cur(); if (c && run.phase === "answer") gradeChoice(i); },
    check: doCheck, cont: doContinue,
    clickOption: i => { const b = document.querySelectorAll(".opt")[i]; if (b) b.click(); },
    autoRun: function (maxSteps) {
      let steps = 0;
      while (run && !run.finalized && steps++ < (maxSteps || 200)) {
        const c = cur();
        if (!c) break;
        if (c.t === "teach" || c.t === "context") { doContinue(); continue; }
        if (c.t === "read") {
          run.readRevealed = run.readRevealed || {}; run.readRevealed[c.id] = true;
          doContinue(); continue;
        }
        if (c.t === "match") {
          for (const p of c.pairs) {
            run.match.selLeft = p.id;
            run.match.done.push(p.id);
            recordAnswer(p.id, true, true);
          }
          run.graded = { ok: true, correctText: "" }; run.phase = "feedback"; doContinue();
          continue;
        }
        if (c.t === "build") {
          const target = c.answer.split(" ");
          const sel = [];
          const used = new Set();
          for (const tk of target) {
            for (let i = 0; i < c.tiles.length; i++) {
              if (!used.has(i) && c.tiles[i] === tk) { used.add(i); sel.push(i); break; }
            }
          }
          run.buildSel = sel; doCheck(); doContinue(); continue;
        }
        gradeChoice(c.answer); doCheck(); doContinue();
      }
      return { steps: steps, finalized: run ? run.finalized : true };
    },
    state: () => st
  };

  /* ================================================================ boot */
  installPressFeedback();
  render();
})();
