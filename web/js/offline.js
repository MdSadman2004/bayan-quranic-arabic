/* Bayan — offline status surface. No network dependency of its own: it registers
 * the service worker and reports readiness only once the cached shell is really
 * readable. Exposes window.Bayan.offline { ready, status, error } and dispatches
 * a "bayan:offline" event on every change.
 */
(function () {
  "use strict";
  const CACHE_PREFIX = "bayan-app-";
  const SHELL = "index.html";
  const BAYAN = window.Bayan || {};

  let ready = false;
  let status = "pending";
  let error = null;
  let checking = false;

  function emit() {
    try {
      if (typeof window.dispatchEvent === "function" && typeof window.CustomEvent === "function") {
        window.dispatchEvent(new window.CustomEvent("bayan:offline", {
          detail: { ready: ready, status: status, error: error }
        }));
      }
    } catch (ignored) { /* observers must never break the page */ }
  }

  function supported() {
    return typeof navigator !== "undefined" && "serviceWorker" in navigator
      && typeof caches !== "undefined";
  }

  async function cachedShell() {
    try {
      const names = await caches.keys();
      for (const name of names) {
        if (!name.startsWith(CACHE_PREFIX)) continue;
        const cache = await caches.open(name);
        const hit = await cache.match(new URL(SHELL, window.location.href).href);
        if (hit) return true;
      }
    } catch (ignored) { /* cache inspection is best effort */ }
    return false;
  }

  async function refresh() {
    if (checking) return ready;
    checking = true;
    try {
      if (!supported()) {
        ready = false; status = "unavailable"; error = "This browser cannot keep an offline copy.";
      } else {
        await navigator.serviceWorker.register("./sw.js");
        if (await cachedShell()) {
          ready = true; status = "ready"; error = null;
        } else {
          // Registration resolves before the install finishes caching.
          ready = false; status = "pending"; error = null;
        }
      }
    } catch (cause) {
      ready = false; status = "error";
      error = "Offline copy could not be prepared. The app still works online.";
    } finally {
      checking = false;
      emit();
    }
    return ready;
  }

  BAYAN.offline = {
    get ready() { return ready; },
    get status() { return status; },
    get error() { return error; },
    refresh: refresh,
    probe: async function () {
      return { ready: await cachedShell(), status: status, supported: supported() };
    }
  };

  if (!supported()) {
    status = "unavailable";
    error = "This browser cannot keep an offline copy.";
    emit();
  } else {
    refresh().then(() => setTimeout(refresh, 1200));
  }

  window.Bayan = BAYAN;
})();
