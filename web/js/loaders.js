/* Bayan — contemplative loaders and micro-interactions (features 19–24).
 * Each returns markup with its own CSS-driven motion, so a wait becomes a
 * short pause instead of a spinner. Pure string builders: no timers, no state.
 */
(function () {
  "use strict";
  const BAYAN = window.Bayan || {};
  const F = BAYAN.F || {};

  const STAR = '<svg viewBox="0 0 120 120" class="ld-star" aria-hidden="true">' +
    '<g class="ld-star-g">' +
    ['0', '45', '90', '135'].map(function (deg) {
      return '<rect x="52" y="14" width="16" height="92" rx="7" transform="rotate(' + deg + ' 60 60)"/>';
    }).join('') +
    '</g><circle cx="60" cy="60" r="15" class="ld-star-core"/></svg>';

  function starBreath(label) {
    return '<div class="loader ld-breath" role="status" aria-live="polite">' + STAR +
      (label ? '<p class="ld-label">' + label + '</p>' : '') + '</div>';
  }

  function rootSprout(root, words, label) {
    const letters = String(root || '').split(' ').filter(Boolean);
    const stem = letters.map(function (letter, index) {
      return '<span class="ld-root-letter" style="--i:' + index + '">' + letter + '</span>';
    }).join('');
    const branches = (words || []).slice(0, 3).map(function (word, index) {
      return '<span class="ld-branch" style="--i:' + index + '">' + word + '</span>';
    }).join('');
    return '<div class="loader ld-sprout" role="status" aria-live="polite">' +
      '<div class="ld-soil"></div><div class="ld-roots">' + stem + '</div>' +
      '<div class="ld-branches">' + branches + '</div>' +
      (label ? '<p class="ld-label">' + label + '</p>' : '') + '</div>';
  }

  function mushafShimmer(lines) {
    const rows = Math.max(3, Math.min(8, lines || 4));
    let out = '<div class="loader ld-mushaf" role="status" aria-live="polite">';
    for (let i = 0; i < rows; i++) out += '<div class="ld-line" style="--i:' + i + '"></div>';
    return out + '</div>';
  }

  function qalamStroke(arabic) {
    return '<span class="ld-qalam" aria-hidden="true">' +
      '<svg viewBox="0 0 300 80" preserveAspectRatio="none" class="ld-qalam-ink">' +
      '<path d="M292 48 C 236 22, 176 62, 120 40 C 78 24, 40 46, 8 32" pathLength="1"/>' +
      '</svg><span class="ar ld-qalam-text" dir="rtl" lang="ar">' + arabic + '</span></span>';
  }

  function skyShader(date) {
    const kind = F.skyForDate ? F.skyForDate(date) : 'night';
    return '<div class="sky ' + ('sky-' + kind) + '" aria-hidden="true">' +
      '<span class="sky-lantern"></span><span class="sky-stars"></span></div>';
  }

  function whisperCard(whisper, lang, skipLabel) {
    if (!whisper) return '';
    const line = (whisper.line && (whisper.line[lang] || whisper.line.en)) || '';
    return '<div class="loader ld-whisper" role="status">' +
      '<span class="ld-whisper-root ar" dir="rtl" lang="ar">' + (whisper.root || '') + '</span>' +
      '<p class="ld-whisper-line">' + line + '</p>' +
      '<button class="ld-skip" type="button" data-skip-loader>' + (skipLabel || 'Skip') + '</button>' +
      '</div>';
  }

  /* Renders a loader into a container and fades it out; the timer is cleared by
   * the caller's next render, which replaces the container's markup. */
  function show(container, markup, ms) {
    if (!container) return null;
    container.innerHTML = '<div class="loader-layer">' + markup + '</div>';
    if (!ms) return null;
    return setTimeout(function () {
      const layer = container.querySelector('.loader-layer');
      if (layer) layer.classList.add('ld-out');
    }, ms);
  }

  BAYAN.Loaders = {
    starBreath: starBreath,
    rootSprout: rootSprout,
    mushafShimmer: mushafShimmer,
    qalamStroke: qalamStroke,
    skyShader: skyShader,
    whisperCard: whisperCard,
    show: show
  };
  window.Bayan = BAYAN;
})();
