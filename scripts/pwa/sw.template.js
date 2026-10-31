/* global self, caches, fetch, Response, URL, __FILES__, __VERSION__, __BYTES__ */
/**
 * Offline service worker. scripts/pwa/build-sw.ts fills in FILES (every built file with a content
 * hash) and VERSION, then writes this to dist/sw.js after Pagefind has run.
 *
 * - Install downloads the whole site into a new cache. Files whose hash didn't change are copied
 *   from the previous cache, so an update only downloads what changed.
 * - Every request is answered from the cache first; the network is only a fallback.
 * - The page can ask for a status report, a repair (fetch anything missing) or a full re-download.
 */
const FILES = __FILES__;
const VERSION = __VERSION__;
const BYTES = __BYTES__; // total size of FILES, shown on the offline page

const PREFIX = 'efg-';
const CACHE = `${PREFIX}${VERSION}`;
const HASHES = '__hashes__'; // a cached JSON document: url → hash, for incremental updates
const SCOPE = new URL(self.registration.scope);
const abs = (path) => new URL(path, SCOPE).href;
const NOT_FOUND = abs('404.html');
const PARALLEL = 6;

let busy = null; // the running download, so two requests don't race

async function post(msg) {
  const clients = await self.clients.matchAll({ includeUncontrolled: true });
  for (const c of clients) c.postMessage(msg);
}

/** Fill `cache` with every file; `reuse` (url → Response) supplies unchanged files. */
async function download(cache, { reuse, force = false, kind }) {
  const entries = Object.entries(FILES);
  let done = 0;
  let fetched = 0;
  let lastPost = 0;
  const queue = entries.slice();
  const report = (final) => {
    const now = Date.now();
    if (!final && now - lastPost < 150) return;
    lastPost = now;
    post({ type: 'progress', kind, done, total: entries.length, fetched });
  };
  async function worker() {
    while (queue.length) {
      const [path, hash] = queue.shift();
      const url = abs(path);
      let res = force ? null : await reuse(url, hash);
      if (!res) {
        // Bypass the HTTP cache so a re-download really comes from the server.
        res = await fetch(url, { cache: 'reload' });
        if (!res.ok) throw new Error(`${res.status} ${path}`);
        fetched++;
      }
      await cache.put(url, res);
      done++;
      report(false);
    }
  }
  await Promise.all(Array.from({ length: PARALLEL }, worker));
  await cache.put(abs(HASHES), new Response(JSON.stringify(FILES)));
  report(true);
}

/** Older caches of this site, newest first by key order. */
async function oldCaches() {
  return (await caches.keys()).filter((k) => k.startsWith(PREFIX) && k !== CACHE);
}

async function reuseFrom(names) {
  const sources = [];
  for (const name of names) {
    const c = await caches.open(name);
    // This version's own cache holds files with this version's hashes, even before HASHES is written.
    const r = name === CACHE ? null : await c.match(abs(HASHES));
    if (name === CACHE) sources.push({ cache: c, hashes: FILES });
    else if (r) sources.push({ cache: c, hashes: await r.json() });
  }
  return async (url, hash) => {
    const path = url.slice(SCOPE.href.length);
    for (const s of sources) {
      if (s.hashes[path] !== hash) continue;
      const hit = await s.cache.match(url);
      if (hit) return hit;
    }
    return null;
  };
}

self.addEventListener('install', (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(CACHE);
      // Include this version's own partial cache: a failed install resumes instead of restarting.
      const reuse = await reuseFrom([CACHE, ...(await oldCaches())]);
      busy = download(cache, { reuse, kind: 'install' });
      try {
        await busy;
      } catch (err) {
        post({ type: 'error', message: String(err) });
        throw err;
      } finally {
        busy = null;
      }
      const first = (await oldCaches()).length === 0;
      // The first install has no older version to replace, so take over straight away.
      if (first) await self.skipWaiting();
    })(),
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      for (const name of await oldCaches()) await caches.delete(name);
      await self.clients.claim();
      post({ type: 'ready', version: VERSION });
    })(),
  );
});

/** Map a request to the file it would be served from: /topic/ → /topic/index.html. */
function fileFor(url) {
  if (!url.href.startsWith(SCOPE.href)) return null;
  let path = url.pathname.slice(SCOPE.pathname.length);
  if (path === '' || path.endsWith('/')) path += 'index.html';
  else if (!(path in FILES) && `${path}/index.html` in FILES) path += '/index.html';
  return path in FILES ? abs(path) : null;
}

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== SCOPE.origin) return;
  const file = fileFor(url);
  const navigate = req.mode === 'navigate';
  if (!file && !navigate) return;
  event.respondWith(
    (async () => {
      const cache = await caches.open(CACHE);
      if (file) {
        const hit = await cache.match(file);
        if (hit) return hit;
      }
      try {
        return await fetch(req);
      } catch {
        // Offline and not cached: an unknown page gets the site's own 404.
        const page = navigate ? await cache.match(NOT_FOUND) : null;
        if (!page) return Response.error();
        return new Response(page.body, { status: 404, headers: page.headers });
      }
    })(),
  );
});

async function status() {
  const cache = await caches.open(CACHE);
  const keys = new Set((await cache.keys()).map((r) => r.url));
  const missing = Object.keys(FILES).filter((p) => !keys.has(abs(p)));
  return {
    type: 'status',
    version: VERSION,
    total: Object.keys(FILES).length,
    missing: missing.length,
    bytes: BYTES,
  };
}

self.addEventListener('message', (event) => {
  const { type } = event.data ?? {};
  const reply = (msg) => event.source?.postMessage(msg);
  if (type === 'status') {
    event.waitUntil(status().then(reply));
  } else if (type === 'skip-waiting') {
    self.skipWaiting();
  } else if (type === 'repair' || type === 'resync') {
    // repair: fetch only what's missing. resync: download every file again from the server.
    event.waitUntil(
      (async () => {
        if (busy) return reply({ type: 'error', message: 'A download is already running.' });
        const cache = await caches.open(CACHE);
        const reuse = async (url) => (await cache.match(url)) ?? null;
        busy = download(cache, { reuse, force: type === 'resync', kind: type });
        try {
          await busy;
          reply(await status());
        } catch (err) {
          reply({ type: 'error', message: String(err) });
        } finally {
          busy = null;
        }
      })(),
    );
  }
});
