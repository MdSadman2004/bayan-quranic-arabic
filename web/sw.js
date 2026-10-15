/* Bayan — service worker TEMPLATE. Rendered into web/sw.js by
 * tools/build_offline.py, which substitutes e30f9f8f7d0b and [
  "css/style.css",
  "fonts/f1.woff2",
  "fonts/f10.woff2",
  "fonts/f11.woff2",
  "fonts/f12.woff2",
  "fonts/f13.woff2",
  "fonts/f14.woff2",
  "fonts/f15.woff2",
  "fonts/f16.woff2",
  "fonts/f17.woff2",
  "fonts/f18.woff2",
  "fonts/f2.woff2",
  "fonts/f3.woff2",
  "fonts/f4.woff2",
  "fonts/f5.woff2",
  "fonts/f6.woff2",
  "fonts/f7.woff2",
  "fonts/f8.woff2",
  "fonts/f9.woff2",
  "fonts/fonts.css",
  "icon-192.png",
  "icon-512.png",
  "icon.svg",
  "index.html",
  "js/app.js",
  "js/audio.js",
  "js/data.js",
  "js/engine.js",
  "js/features-ui.js",
  "js/features.js",
  "js/i18n.js",
  "js/loaders.js",
  "js/offline.js",
  "js/storage.js",
  "manifest.webmanifest"
].
 * The whole app is cached locally; if any file fails to cache the install
 * aborts and the previous complete copy keeps serving, so the app never boots
 * into a half-populated offline state.
 */
const VERSION = "e30f9f8f7d0b";
const CACHE = "bayan-app-" + VERSION;
const FILES = [
  "css/style.css",
  "fonts/f1.woff2",
  "fonts/f10.woff2",
  "fonts/f11.woff2",
  "fonts/f12.woff2",
  "fonts/f13.woff2",
  "fonts/f14.woff2",
  "fonts/f15.woff2",
  "fonts/f16.woff2",
  "fonts/f17.woff2",
  "fonts/f18.woff2",
  "fonts/f2.woff2",
  "fonts/f3.woff2",
  "fonts/f4.woff2",
  "fonts/f5.woff2",
  "fonts/f6.woff2",
  "fonts/f7.woff2",
  "fonts/f8.woff2",
  "fonts/f9.woff2",
  "fonts/fonts.css",
  "icon-192.png",
  "icon-512.png",
  "icon.svg",
  "index.html",
  "js/app.js",
  "js/audio.js",
  "js/data.js",
  "js/engine.js",
  "js/features-ui.js",
  "js/features.js",
  "js/i18n.js",
  "js/loaders.js",
  "js/offline.js",
  "js/storage.js",
  "manifest.webmanifest"
];

function inScope(url) {
  const root = self.location.pathname.replace(/[^/]*$/, "");
  return url.pathname.startsWith(root);
}

async function cacheAll() {
  const cache = await caches.open(CACHE);
  const missing = [];
  for (const file of FILES) {
    try {
      const response = await fetch(file, { cache: "no-cache" });
      if (!response || !response.ok) { missing.push(file); continue; }
      await cache.put(file, response);
    } catch (error) {
      missing.push(file);
    }
  }
  return missing;
}

self.addEventListener("install", (event) => {
  event.waitUntil((async () => {
    const missing = await cacheAll();
    if (missing.length) {
      console.warn("[Bayan offline] install incomplete; keeping the previous copy", missing);
      return;
    }
    await self.skipWaiting();
  })());
});

self.addEventListener("activate", (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys
      .filter((key) => key.startsWith("bayan-app-") && key !== CACHE)
      .map((key) => caches.delete(key)));
    await self.clients.claim();
  })());
});

async function respond(request, url) {
  const cache = await caches.open(CACHE);
  const cached = await cache.match(request.url);
  if (cached) return cached;
  try {
    const response = await fetch(request);
    if (response && response.ok) await cache.put(request.url, response.clone());
    return response;
  } catch (error) {
    if (request.mode === "navigate") {
      const shell = await cache.match(new URL("index.html", url).href);
      if (shell) return shell;
    }
    return new Response("Offline", { status: 503, headers: { "Content-Type": "text/plain" } });
  }
}

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;
  let url;
  try { url = new URL(request.url); } catch (error) { return; }
  if (url.origin !== self.location.origin) return;
  if (!inScope(url)) return;
  event.respondWith(respond(request, url));
});
