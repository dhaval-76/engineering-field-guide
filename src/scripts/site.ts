/**
 * Site-wide progressive enhancement. Pages are fully readable without it;
 * this adds theme switching, the mobile drawer, code tabs, copy buttons,
 * progress/bookmarks and keyboard shortcuts.
 */
import { broadcast, keys, read, write, type ProgressMap, type RecentList } from '~/lib/storage';

const root = document.documentElement;

/* ── Theme ─────────────────────────────────────────────── */
function currentTheme(): 'light' | 'dark' {
  const explicit = root.dataset.theme;
  if (explicit === 'light' || explicit === 'dark') return explicit;
  return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

document.querySelectorAll<HTMLButtonElement>('[data-theme-toggle]').forEach((btn) => {
  btn.addEventListener('click', () => {
    const next = currentTheme() === 'dark' ? 'light' : 'dark';
    root.dataset.theme = next;
    write(keys.theme, next);
  });
});

/* ── Mobile navigation drawer ──────────────────────────── */
const navToggle = document.querySelector<HTMLButtonElement>('[data-nav-toggle]');
const scrim = document.querySelector<HTMLElement>('[data-nav-close]');

function setNav(open: boolean) {
  if (open) root.dataset.nav = 'open';
  else delete root.dataset.nav;
  navToggle?.setAttribute('aria-expanded', String(open));
  if (scrim) scrim.hidden = !open;
  document.body.style.overflow = open ? 'hidden' : '';
}

navToggle?.addEventListener('click', () => setNav(root.dataset.nav !== 'open'));
scrim?.addEventListener('click', () => setNav(false));
matchMedia('(min-width: 960px)').addEventListener('change', (e) => e.matches && setNav(false));

/* Keep the current topic visible in the sidebar. */
document.querySelector('#sidebar [aria-current="page"]')?.scrollIntoView({ block: 'center' });

/* ── Code tabs: one global language preference ─────────── */
function applyLanguage(lang: string) {
  document.querySelectorAll<HTMLElement>('[data-code-tabs]').forEach((group) => {
    const available = (group.dataset.langs ?? '').split(',');
    const chosen = available.includes(lang) ? lang : available[0];
    group.querySelectorAll<HTMLButtonElement>('[role="tab"]').forEach((tab) => {
      const on = tab.dataset.lang === chosen;
      tab.setAttribute('aria-selected', String(on));
      tab.tabIndex = on ? 0 : -1;
    });
    group.querySelectorAll<HTMLElement>('[role="tabpanel"]').forEach((panel) => {
      panel.hidden = panel.dataset.lang !== chosen;
    });
    const note = group.querySelector<HTMLElement>('[data-fallback-note]');
    if (note) {
      note.hidden = chosen === lang;
      note.textContent =
        chosen === lang
          ? ''
          : `Not yet available in your preferred language — showing ${group.querySelector(`[role="tab"][data-lang="${chosen}"]`)?.textContent ?? chosen}.`;
    }
  });
}

const savedLang = read<string | null>(keys.lang, null);
if (savedLang) applyLanguage(savedLang);

document.addEventListener('click', (e) => {
  const tab = (e.target as Element).closest<HTMLButtonElement>('[data-code-tabs] [role="tab"]');
  if (!tab?.dataset.lang) return;
  write(keys.lang, tab.dataset.lang);
  root.dataset.lang = tab.dataset.lang;
  applyLanguage(tab.dataset.lang);
});

document.addEventListener('keydown', (e) => {
  const tab = (e.target as Element).closest?.<HTMLButtonElement>('[data-code-tabs] [role="tab"]');
  if (!tab || (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft')) return;
  const tabs = [...tab.parentElement!.querySelectorAll<HTMLButtonElement>('[role="tab"]')];
  const i = tabs.indexOf(tab) + (e.key === 'ArrowRight' ? 1 : -1);
  const next = tabs[(i + tabs.length) % tabs.length]!;
  next.focus();
  next.click();
});

/* ── Copy buttons on code blocks ───────────────────────── */
document.addEventListener('click', async (e) => {
  const btn = (e.target as Element).closest<HTMLButtonElement>('[data-copy]');
  if (!btn) return;
  const scope = btn.closest('[data-copy-scope]');
  const panel = scope?.querySelector<HTMLElement>('[role="tabpanel"]:not([hidden]) pre, pre');
  if (!panel) return;
  try {
    await navigator.clipboard.writeText(panel.innerText);
    btn.dataset.state = 'copied';
    btn.setAttribute('aria-label', 'Copied');
    setTimeout(() => {
      delete btn.dataset.state;
      btn.setAttribute('aria-label', 'Copy code');
    }, 1600);
  } catch {
    /* clipboard blocked; user can still select manually */
  }
});

/* ── Progress & bookmarks ──────────────────────────────── */
function renderProgress() {
  const progress = read<ProgressMap>(keys.progress, {});
  document.querySelectorAll<HTMLElement>('[data-topic-link]').forEach((a) => {
    if (progress[a.dataset.topicLink!]) a.dataset.done = '';
    else delete a.dataset.done;
  });
  document.querySelectorAll<HTMLElement>('[data-track-count]').forEach((el) => {
    const track = el.dataset.trackCount!;
    const done = Object.keys(progress).filter((id) => id.startsWith(`${track}/`)).length;
    const total = el.dataset.total ?? '0';
    el.textContent = done ? `${done}/${total}` : total;
  });
  document.querySelectorAll<HTMLButtonElement>('[data-progress-toggle]').forEach((btn) => {
    const done = Boolean(progress[btn.dataset.progressToggle!]);
    btn.setAttribute('aria-pressed', String(done));
    btn.querySelector('[data-label]')!.textContent = done ? 'Completed' : 'Mark complete';
  });
  const bookmarks = read<string[]>(keys.bookmarks, []);
  document.querySelectorAll<HTMLButtonElement>('[data-bookmark-toggle]').forEach((btn) => {
    const on = bookmarks.includes(btn.dataset.bookmarkToggle!);
    btn.setAttribute('aria-pressed', String(on));
    btn.querySelector('[data-label]')!.textContent = on ? 'Saved' : 'Save';
  });
}

document.addEventListener('click', (e) => {
  const target = e.target as Element;
  const p = target.closest<HTMLButtonElement>('[data-progress-toggle]');
  if (p) {
    const id = p.dataset.progressToggle!;
    const progress = read<ProgressMap>(keys.progress, {});
    if (progress[id]) delete progress[id];
    else progress[id] = Date.now();
    write(keys.progress, progress);
    broadcast(keys.progress);
  }
  const b = target.closest<HTMLButtonElement>('[data-bookmark-toggle]');
  if (b) {
    const id = b.dataset.bookmarkToggle!;
    const list = read<string[]>(keys.bookmarks, []);
    write(keys.bookmarks, list.includes(id) ? list.filter((x) => x !== id) : [id, ...list]);
    broadcast(keys.bookmarks);
  }
});

window.addEventListener('efg:storage', renderProgress);
window.addEventListener('storage', renderProgress);
renderProgress();

/* Remember recently viewed topics for the home page. */
const topicMeta = document.querySelector<HTMLElement>('[data-topic-id]');
if (topicMeta) {
  const id = topicMeta.dataset.topicId!;
  const title = topicMeta.dataset.topicTitle ?? id;
  const recent = read<RecentList>(keys.recent, []).filter((r) => r.id !== id);
  write(keys.recent, [{ id, title, at: Date.now() }, ...recent].slice(0, 8));
}

/* ── Focus mode ────────────────────────────────────────── */
document.querySelectorAll<HTMLButtonElement>('[data-focus-toggle]').forEach((btn) => {
  const sync = () => btn.setAttribute('aria-pressed', String(root.dataset.focus === 'on'));
  sync();
  btn.addEventListener('click', () => {
    const on = root.dataset.focus !== 'on';
    if (on) root.dataset.focus = 'on';
    else delete root.dataset.focus;
    write(keys.focus, on);
    sync();
  });
});

/* ── Table of contents: highlight the section in view ──── */
const tocLinks = [...document.querySelectorAll<HTMLAnchorElement>('[data-toc] a[href^="#"]')];
if (tocLinks.length && 'IntersectionObserver' in window) {
  const byId = new Map(tocLinks.map((a) => [decodeURIComponent(a.hash.slice(1)), a]));
  const headings = [...byId.keys()]
    .map((id) => document.getElementById(id))
    .filter((h): h is HTMLElement => !!h);
  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        tocLinks.forEach((a) => a.removeAttribute('aria-current'));
        byId.get(entry.target.id)?.setAttribute('aria-current', 'true');
      }
    },
    { rootMargin: '-64px 0px -70% 0px' },
  );
  headings.forEach((h) => observer.observe(h));
}

/* ── Keyboard shortcuts ────────────────────────────────── */
document.addEventListener('keydown', (e) => {
  if (e.defaultPrevented || e.metaKey || e.ctrlKey || e.altKey) return;
  const el = e.target as HTMLElement;
  if (el.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName)) return;
  if (e.key === 'Escape' && root.dataset.nav === 'open') setNav(false);
  const rel = e.key === 'j' ? 'next' : e.key === 'k' ? 'prev' : null;
  if (rel) {
    const link = document.querySelector<HTMLAnchorElement>(`a[rel="${rel}"]`);
    if (link) location.href = link.href;
  }
});

/* ── Scrollable tables ─────────────────────────────────── */
// Wide tables scroll inside themselves on narrow screens; keyboard users need to focus them to scroll.
/* Label every cell with its column header so prose.css can show wide tables as cards on phones. */
document.querySelectorAll<HTMLTableElement>('.prose table').forEach((t) => {
  const heads = [...t.querySelectorAll('thead th')].map((th) => th.textContent?.trim() ?? '');
  const wide = heads.length >= 3 || t.scrollWidth > t.clientWidth + 1;
  if (!wide || !heads.some(Boolean)) return;
  t.classList.add('stackable');
  t.querySelectorAll('tbody tr').forEach((tr) =>
    [...tr.children].forEach((td, i) => heads[i] && td.setAttribute('data-label', heads[i]!)),
  );
});

function markScrollableTables() {
  document.querySelectorAll<HTMLTableElement>('.prose table').forEach((t) => {
    if (t.scrollWidth > t.clientWidth + 1) {
      t.tabIndex = 0;
      if (!t.hasAttribute('aria-label')) {
        const heading = t.closest('section, .prose')?.querySelector('h2, h3');
        t.setAttribute('aria-label', `Table${heading ? `: ${heading.textContent?.trim()}` : ''} (scrolls sideways)`);
      }
    } else {
      t.removeAttribute('tabindex');
    }
  });
}
markScrollableTables();
let resizeTimer: number | undefined;
addEventListener('resize', () => {
  clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(markScrollableTables, 200);
});

/* ── Diagram viewer: full screen, zoomable, pannable (phones need this most) ── */
let viewer: HTMLDialogElement | null = null;
function openDiagram(figure: HTMLElement) {
  const source = figure.querySelector<SVGSVGElement>('.canvas > svg');
  if (!source) return;
  if (!viewer) {
    viewer = document.createElement('dialog');
    viewer.className = 'dviewer';
    viewer.innerHTML = `
      <div class="dviewer-bar">
        <p class="dviewer-title"></p>
        <button type="button" data-zoom="-1" aria-label="Zoom out">−</button>
        <button type="button" data-zoom="0" aria-label="Fit to screen">Fit</button>
        <button type="button" data-zoom="1" aria-label="Zoom in">+</button>
        <button type="button" data-close aria-label="Close diagram">Close</button>
      </div>
      <div class="dviewer-body" tabindex="0"></div>`;
    document.body.append(viewer);
    viewer.addEventListener('close', () => viewer!.querySelector('.dviewer-body')!.replaceChildren());
    viewer.addEventListener('click', (e) => {
      const t = e.target as HTMLElement;
      if (t === viewer || t.closest('[data-close]')) viewer!.close();
      const z = t.closest<HTMLElement>('[data-zoom]')?.dataset.zoom;
      if (z !== undefined) zoom(Number(z));
    });
  }
  const body = viewer.querySelector<HTMLElement>('.dviewer-body')!;
  const svg = source.cloneNode(true) as SVGSVGElement;
  const natural = Number(svg.getAttribute('viewBox')?.split(/\s+/)[2]) || source.getBoundingClientRect().width;
  svg.dataset.natural = String(natural);
  svg.removeAttribute('style');
  body.replaceChildren(svg);
  viewer.querySelector('.dviewer-title')!.textContent =
    figure.querySelector('figcaption span')?.textContent ?? '';
  viewer.showModal();
  // Start at the diagram's natural size (readable text), or fit if it is narrower than the screen.
  setScale(Math.max(1, fitScale()));
  body.scrollTo(0, 0);
}
function fitScale() {
  const body = viewer!.querySelector<HTMLElement>('.dviewer-body')!;
  const svg = body.querySelector<SVGSVGElement>('svg')!;
  return (body.clientWidth - 32) / Number(svg.dataset.natural);
}
function setScale(s: number) {
  const svg = viewer!.querySelector<SVGSVGElement>('.dviewer-body svg')!;
  const scale = Math.min(4, Math.max(0.2, s));
  svg.dataset.scale = String(scale);
  svg.style.width = `${Math.round(Number(svg.dataset.natural) * scale)}px`;
}
function zoom(dir: number) {
  const svg = viewer!.querySelector<SVGSVGElement>('.dviewer-body svg')!;
  const current = Number(svg.dataset.scale) || 1;
  setScale(dir === 0 ? fitScale() : current * (dir > 0 ? 1.25 : 0.8));
}
document.addEventListener('click', (e) => {
  const btn = (e.target as Element).closest('[data-diagram-expand]');
  const fig = btn?.closest<HTMLElement>('[data-diagram]');
  if (fig) openDiagram(fig);
});

/* ── Code line wrapping: one global preference ─────────── */
function syncWrap() {
  const on = root.dataset.wrap === 'on';
  document.querySelectorAll('[data-wrap-toggle]').forEach((b) => b.setAttribute('aria-pressed', String(on)));
}
syncWrap();
document.addEventListener('click', (e) => {
  if (!(e.target as Element).closest('[data-wrap-toggle]')) return;
  const on = root.dataset.wrap !== 'on';
  root.dataset.wrap = on ? 'on' : 'off';
  write(keys.wrap, on);
  syncWrap();
});
