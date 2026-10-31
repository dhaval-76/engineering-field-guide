/**
 * Offline support: registers the service worker (scripts/pwa/sw.template.js), which saves the whole
 * site on first visit. Keeps a small state that the header indicator, the update prompt and the
 * /offline/ page render from.
 */
import { read, write } from '~/lib/storage';

export type Phase =
  | 'unsupported' // no service workers (or a dev build)
  | 'starting'
  | 'paused' // data saver is on: wait for the reader to start the download
  | 'installing' // first download
  | 'ready'
  | 'updating' // downloading a new version in the background
  | 'update-ready' // new version downloaded, waiting for a reload
  | 'repair'
  | 'resync'
  | 'removed'
  | 'error';

export interface OfflineState {
  phase: Phase;
  online: boolean;
  done: number;
  total: number;
  /** Files actually downloaded in the last run (the rest were reused from the previous version). */
  fetched: number;
  version?: string | undefined;
  missing?: number | undefined;
  bytes?: number | undefined;
  usage?: number | undefined;
  persisted?: boolean | undefined;
  lastChecked?: number | undefined;
  note?: string | undefined;
  error?: string | undefined;
}

const BASE = import.meta.env.BASE_URL.replace(/\/?$/, '/');
const LAST_CHECK = 'offline-checked';
const CHECK_EVERY = 10 * 60 * 1000;

const state: OfflineState = {
  phase: 'starting',
  online: navigator.onLine,
  done: 0,
  total: 0,
  fetched: 0,
  lastChecked: read<number | undefined>(LAST_CHECK, undefined),
};
const listeners = new Set<(s: OfflineState) => void>();
let reg: ServiceWorkerRegistration | undefined;
let reloading = false;

export const getState = () => state;
export function subscribe(fn: (s: OfflineState) => void) {
  listeners.add(fn);
  fn(state);
  return () => listeners.delete(fn);
}
function set(patch: Partial<OfflineState>) {
  Object.assign(state, patch);
  listeners.forEach((fn) => fn(state));
}

const sw = () => ('serviceWorker' in navigator ? navigator.serviceWorker : undefined);

async function refreshStorage() {
  try {
    const { usage } = await navigator.storage.estimate();
    set({ usage, persisted: await navigator.storage.persisted() });
  } catch {
    /* storage API unavailable */
  }
}

function askStatus() {
  sw()?.controller?.postMessage({ type: 'status' });
}

function onMessage(e: MessageEvent) {
  const msg = e.data ?? {};
  if (msg.type === 'progress') {
    const phase: Phase =
      msg.kind === 'install' ? (sw()?.controller ? 'updating' : 'installing') : msg.kind;
    set({ phase, done: msg.done, total: msg.total, fetched: msg.fetched, error: undefined });
  } else if (msg.type === 'status') {
    const keep = state.phase === 'update-ready' || state.phase === 'updating';
    set({
      phase: keep ? state.phase : 'ready',
      version: msg.version,
      total: msg.total,
      missing: msg.missing,
      bytes: msg.bytes,
    });
    refreshStorage();
  } else if (msg.type === 'ready') {
    askStatus();
  } else if (msg.type === 'error') {
    set({ phase: 'error', error: msg.message });
  }
}

/** Watch a worker that is downloading; `first` when there is no older version to replace. */
function track(worker: ServiceWorker) {
  const first = !sw()?.controller;
  set({ phase: first ? 'installing' : 'updating' });
  worker.addEventListener('statechange', () => {
    if (worker.state === 'installed' && !first) set({ phase: 'update-ready' });
    if (worker.state === 'redundant' && state.phase !== 'ready' && state.phase !== 'update-ready')
      set({ phase: 'error', error: 'The download stopped. It will retry on your next visit.' });
  });
}

async function register() {
  const container = sw();
  if (!container || import.meta.env.DEV) return set({ phase: 'unsupported' });
  container.addEventListener('message', onMessage);
  container.addEventListener('controllerchange', () => {
    if (reloading) location.reload();
    else askStatus();
  });
  reg = await container.register(`${BASE}sw.js`, { scope: BASE, updateViaCache: 'none' });
  if (reg.waiting && container.controller) set({ phase: 'update-ready' });
  else if (reg.installing) track(reg.installing);
  else askStatus();
  // Opening a page online makes the browser fetch sw.js again (updateViaCache: 'none'), which is the check.
  if (navigator.onLine) {
    write(LAST_CHECK, Date.now());
    set({ lastChecked: Date.now() });
  }
  reg.addEventListener('updatefound', () => reg?.installing && track(reg.installing));
  // Ask the browser not to evict the saved copy when space runs low.
  try {
    if (!(await navigator.storage.persisted())) await navigator.storage.persist();
  } catch {
    /* not supported */
  }
  refreshStorage();
}

/** Asks the server for a newer version; a new one starts downloading in the background. */
export async function checkForUpdates() {
  if (!state.online)
    return set({ note: 'You are offline. Updates can be checked once you reconnect.' });
  if (!reg) return;
  set({ note: 'Checking…' });
  try {
    await reg.update();
    const now = Date.now();
    write(LAST_CHECK, now);
    const pending = reg.installing || (reg.waiting && sw()?.controller);
    set({ lastChecked: now, note: pending ? undefined : 'You have the latest version.' });
  } catch {
    set({ note: 'Could not reach the site. Try again when you are online.' });
  }
}

/** Downloads only the files missing from the saved copy (e.g. after the browser cleared some). */
export const repair = () => send('repair');
/** Downloads every file again from the server, replacing the saved copy. */
export const resync = () => send('resync');
function send(type: 'repair' | 'resync') {
  if (!state.online) return set({ note: 'You are offline. Reconnect to download files.' });
  const controller = sw()?.controller;
  if (!controller) return set({ note: 'The first download has not finished yet.' });
  set({ phase: type, done: 0, fetched: 0, note: undefined, error: undefined });
  controller.postMessage({ type });
}

/** Reloads into the downloaded version. */
export function applyUpdate() {
  if (!reg?.waiting) return location.reload();
  reloading = true;
  reg.waiting.postMessage({ type: 'skip-waiting' });
}

/** Starts the first download when data saver had paused it. */
export async function startDownload() {
  write('offline-optin', true);
  await register();
}

/** Deletes the saved copy and stops the worker; the site then works online only. */
export async function removeOffline() {
  const regs = (await sw()?.getRegistrations()) ?? [];
  await Promise.all(regs.map((r) => r.unregister()));
  for (const key of await caches.keys()) if (key.startsWith('efg-')) await caches.delete(key);
  write('offline-optin', false);
  set({ phase: 'removed', version: undefined, missing: undefined, note: undefined });
  refreshStorage();
}

/* ── Startup ───────────────────────────────────────────── */
addEventListener('online', () => set({ online: true }));
addEventListener('offline', () => set({ online: false }));
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState !== 'visible' || !state.online || !reg) return;
  if (Date.now() - (state.lastChecked ?? 0) > CHECK_EVERY) checkForUpdates();
});

{
  const optIn = read<boolean | null>('offline-optin', null);
  const saveData = (navigator as Navigator & { connection?: { saveData?: boolean } }).connection
    ?.saveData;
  const installed = !!sw()?.controller;
  if (optIn === false && !installed) set({ phase: 'removed' });
  else if (saveData && !installed && optIn !== true) set({ phase: 'paused' });
  else register().catch((err) => set({ phase: 'error', error: String(err) }));
}

/* ── Header indicator and update prompt ────────────────── */
const percent = (s: OfflineState) => (s.total ? Math.floor((s.done / s.total) * 100) : 0);

export function describe(s: OfflineState): string {
  switch (s.phase) {
    case 'unsupported':
      return 'Offline reading is not available in this browser.';
    case 'starting':
      return 'Preparing offline copy…';
    case 'paused':
      return 'Data saver is on: offline download is paused.';
    case 'installing':
      return `Saving for offline: ${percent(s)}%`;
    case 'updating':
      return `Downloading an update: ${percent(s)}%`;
    case 'update-ready':
      return 'An update is ready. Reload to use it.';
    case 'repair':
      return `Repairing the offline copy: ${percent(s)}%`;
    case 'resync':
      return `Re-downloading everything: ${percent(s)}%`;
    case 'removed':
      return 'Offline copy removed.';
    case 'error':
      return 'Offline download failed.';
    case 'ready':
      if (s.missing)
        return `${s.missing} saved file${s.missing === 1 ? ' is' : 's are'} missing. Repair restores them.`;
      return s.online ? 'Available offline.' : 'You are offline: reading the saved copy.';
  }
}

subscribe((s) => {
  document.querySelectorAll<HTMLElement>('[data-offline-indicator]').forEach((el) => {
    const busy = ['installing', 'updating', 'repair', 'resync'].includes(s.phase);
    el.dataset.state =
      s.phase === 'ready' && s.missing
        ? 'error'
        : !s.online && s.phase === 'ready'
          ? 'offline'
          : s.phase;
    el.style.setProperty('--pct', `${busy ? percent(s) : 0}%`);
    const text = describe(s);
    el.title = text;
    el.setAttribute('aria-label', `Offline copy: ${text}`);
  });
});

let toast: HTMLElement | null = null;
subscribe((s) => {
  if (s.phase !== 'update-ready') {
    toast?.remove();
    toast = null;
    return;
  }
  if (toast) return;
  toast = document.createElement('div');
  toast.className = 'update-toast';
  toast.setAttribute('role', 'status');
  const changed = s.fetched ? ` (${s.fetched} changed file${s.fetched === 1 ? '' : 's'})` : '';
  toast.innerHTML = `<span>A new version has been downloaded${changed}.</span>
    <button type="button" data-apply>Reload</button>
    <button type="button" data-later>Later</button>`;
  toast.querySelector('[data-apply]')?.addEventListener('click', applyUpdate);
  toast
    .querySelector('[data-later]')
    ?.addEventListener('click', () => toast?.setAttribute('hidden', ''));
  document.body.append(toast);
});
