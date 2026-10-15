/* Bayan audio — Arabic TTS (Web Speech API) + tiny WebAudio feedback chirps. */
(function () {
  "use strict";
  const A = {};
  let ctx = null;
  let voicesTimer = null;
  let cached = false;
  let checked = false;

  function ttsSupported() {
    return typeof window.speechSynthesis !== "undefined" && typeof window.SpeechSynthesisUtterance !== "undefined";
  }
  function arabicVoice() {
    if (!ttsSupported()) return null;
    const vs = window.speechSynthesis.getVoices() || [];
    return vs.find(v => v.localService === true && /^ar(?:\b|[-_])/i.test(v.lang)) || null;
  }
  A.refresh = function () {
    checked = true;
    cached = !!arabicVoice();
    return cached;
  };
  A.canListen = function () {
    if (!ttsSupported()) return false;
    if (!checked) A.refresh();
    return cached;
  };
  A.speak = function (text) {
    if (!ttsSupported() || !text) return;
    const v = arabicVoice();
    if (!v) return;
    try {
      const u = new window.SpeechSynthesisUtterance(text);
      u.voice = v;
      u.lang = (v && v.lang) || "ar-SA";
      u.rate = 0.82;
      u.pitch = 1;
      window.speechSynthesis.cancel();
      window.speechSynthesis.speak(u);
    } catch (e) { /* TTS is best-effort */ }
  };
  if (ttsSupported()) {
    A.refresh();
    // some browsers populate voices asynchronously
    window.speechSynthesis.onvoiceschanged = function () { A.refresh(); };
    voicesTimer = setInterval(function () {
      if (A.refresh()) clearInterval(voicesTimer);
    }, 1200);
    setTimeout(function () { clearInterval(voicesTimer); }, 15000);
  }

  function audioCtx() {
    if (ctx) return ctx;
    const C = window.AudioContext || window.webkitAudioContext;
    if (!C) return null;
    ctx = new C();
    return ctx;
  }
  /* semitones lifts the whole figure along the musical ladder so a flow streak
   * is heard as rising pitch, not just a longer streak. */
  A.beep = function (kind, semitones) {
    try {
      const c = audioCtx();
      if (!c) return;
      if (c.state === "suspended") c.resume();
      const now = c.currentTime;
      const shift = Math.pow(2, (Number.isFinite(semitones) ? semitones : 0) / 12);
      const g = c.createGain();
      g.connect(c.destination);
      const isTap = kind === "tap";
      g.gain.value = isTap ? 0.028 : 0.05;
      const seq = isTap ? [[1180, 0]]
        : kind === "ok" ? [[659, 0], [880, 0.075]]
        : kind === "bad" ? [[196, 0], [147, 0.09]]
        : [[523, 0], [659, 0.09], [784, 0.18], [1046, 0.27]];
      for (const [f, t] of seq) {
        const o = c.createOscillator();
        o.type = kind === "bad" ? "square" : "sine";
        o.frequency.value = f * shift;
        o.connect(g);
        o.start(now + t);
        o.stop(now + t + (isTap ? 0.03 : 0.11));
      }
    } catch (e) { /* sound is best-effort */ }
  };
  window.Bayan = window.Bayan || {};
  window.Bayan.audio = A;
})();
