/**
 * Highlights on topic pages. A highlight is stored as its text plus a little context, and drawn by
 * wrapping the matching text in <mark>. Islands, diagrams and controls are left out of the text so
 * React never finds its DOM changed.
 */
import {
  CONTEXT_CHARS,
  loadHighlights,
  newHighlightId,
  QUOTE_MAX_CHARS,
  saveHighlights,
  type Highlight,
} from '~/lib/highlights';
import { NOTE_MAX_CHARS } from '~/lib/storage';
import { flash } from '~/scripts/shortcuts';

const ROOT = '[data-highlight-root]';
const SKIP =
  'astro-island, svg, script, style, [popover], button:not(.term), .code-tabs .bar';
const MIN_CHARS = 3;

const topicId = document.querySelector<HTMLElement>('[data-topic-id]')?.dataset.topicId;
const roots = [...document.querySelectorAll<HTMLElement>(ROOT)];

interface TextIndex {
  text: string;
  nodes: { node: Text; start: number }[];
}
/** All highlightable text of the page as one string, and which node each part came from. */
function buildIndex(): TextIndex {
  let text = '';
  const nodes: TextIndex['nodes'] = [];
  for (const root of roots) {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT, {
      acceptNode: (n) =>
        n.nodeType === Node.TEXT_NODE
          ? NodeFilter.FILTER_ACCEPT
          : (n as Element).matches(SKIP)
            ? NodeFilter.FILTER_REJECT
            : NodeFilter.FILTER_SKIP,
    });
    while (walker.nextNode()) {
      const node = walker.currentNode as Text;
      nodes.push({ node, start: text.length });
      text += node.data;
    }
  }
  return { text, nodes };
}

const commonEnd = (a: string, b: string) => {
  let n = 0;
  while (n < a.length && n < b.length && a[a.length - 1 - n] === b[b.length - 1 - n]) n++;
  return n;
};
const commonStart = (a: string, b: string) => {
  let n = 0;
  while (n < a.length && n < b.length && a[n] === b[n]) n++;
  return n;
};

/** Where a highlight sits now: the occurrence of its text whose surroundings match best. */
function locate(h: Highlight, { text }: TextIndex): number | null {
  let best: number | null = null;
  let bestScore = -1;
  for (let at = text.indexOf(h.quote); at !== -1; at = text.indexOf(h.quote, at + 1)) {
    const before = text.slice(Math.max(0, at - CONTEXT_CHARS), at);
    const after = text.slice(at + h.quote.length, at + h.quote.length + CONTEXT_CHARS);
    const score = commonEnd(before, h.prefix) + commonStart(after, h.suffix);
    if (score > bestScore) {
      best = at;
      bestScore = score;
    }
  }
  return best;
}

function draw(id: string, h: Highlight, start: number, index: TextIndex) {
  const end = start + h.quote.length;
  for (const { node, start: nodeStart } of index.nodes) {
    const from = Math.max(start - nodeStart, 0);
    const to = Math.min(end - nodeStart, node.length);
    // Spaces between block elements can't be wrapped: a <mark> isn't allowed directly in a <ul>.
    if (from >= to || !node.data.slice(from, to).trim()) continue;
    const range = document.createRange();
    range.setStart(node, from);
    range.setEnd(node, to);
    const mark = document.createElement('mark');
    mark.className = h.note ? 'hl has-note' : 'hl';
    mark.dataset.hl = id;
    if (h.note) mark.title = h.note;
    range.surroundContents(mark);
  }
}

const marksOf = (id: string) =>
  [...document.querySelectorAll<HTMLElement>('mark.hl')].filter((m) => m.dataset.hl === id);

function erase(id: string) {
  for (const mark of marksOf(id)) {
    const parent = mark.parentNode!;
    mark.replaceWith(...mark.childNodes);
    parent.normalize();
  }
}

function drawAll() {
  let lost = 0;
  const entries = Object.entries(loadHighlights())
    .filter(([, h]) => h.topic === topicId)
    .sort((a, b) => a[1].at - b[1].at);
  for (const [id, h] of entries) {
    const index = buildIndex();
    const at = locate(h, index);
    if (at === null) lost++;
    else draw(id, h, at, index);
  }
  const note = document.querySelector<HTMLElement>('[data-hl-lost]');
  if (note) {
    note.hidden = !lost;
    note.querySelector('span')!.textContent =
      `${lost} of your highlights ${lost === 1 ? 'no longer matches' : 'no longer match'} the text on this page.`;
  }
}

/* ── Reading the selection ─────────────────────────────── */
interface Picked {
  start: number;
  quote: string;
  prefix: string;
  suffix: string;
  range: Range;
}

function readSelection(): Picked | null {
  const selection = getSelection();
  if (!selection || selection.isCollapsed || !selection.rangeCount) return null;
  const range = selection.getRangeAt(0);
  const common = range.commonAncestorContainer;
  const el = common instanceof Element ? common : common.parentElement;
  if (!el?.closest(ROOT) || el.closest(SKIP)) return null;

  const index = buildIndex();
  let start = -1;
  let end = -1;
  for (const { node, start: nodeStart } of index.nodes) {
    if (!range.intersectsNode(node)) continue;
    const from = node === range.startContainer ? range.startOffset : 0;
    const to = node === range.endContainer ? range.endOffset : node.length;
    if (from >= to) continue;
    if (start < 0) start = nodeStart + from;
    end = nodeStart + to;
  }
  if (start < 0) return null;
  const raw = index.text.slice(start, end);
  start += raw.length - raw.trimStart().length;
  end -= raw.length - raw.trimEnd().length;
  const quote = index.text.slice(start, end);
  if (quote.length < MIN_CHARS || quote.length > QUOTE_MAX_CHARS) return null;
  return {
    start,
    quote,
    prefix: index.text.slice(Math.max(0, start - CONTEXT_CHARS), start),
    suffix: index.text.slice(end, end + CONTEXT_CHARS),
    range: range.cloneRange(),
  };
}

const overlapsExisting = (range: Range) =>
  [...document.querySelectorAll('mark.hl')].some((m) => range.intersectsNode(m));

/* ── Saving ────────────────────────────────────────────── */
function save(mutate: (map: ReturnType<typeof loadHighlights>) => void): boolean {
  const map = loadHighlights();
  mutate(map);
  try {
    saveHighlights(map);
    return true;
  } catch {
    flash('Could not save: this browser is blocking storage or is out of space.');
    return false;
  }
}

function addHighlight(p: Picked, note?: string) {
  if (overlapsExisting(p.range)) {
    flash('That overlaps a highlight. Remove it first.');
    return;
  }
  const id = newHighlightId();
  const h: Highlight = {
    topic: topicId!,
    quote: p.quote,
    prefix: p.prefix,
    suffix: p.suffix,
    at: Date.now(),
    ...(note ? { note } : {}),
  };
  if (!save((map) => (map[id] = h))) return;
  draw(id, h, p.start, buildIndex());
  getSelection()?.removeAllRanges();
  flash(note ? 'Note saved' : 'Highlighted');
}

function setNote(id: string, note: string) {
  const done = save((map) => {
    const h = map[id];
    if (!h) return;
    if (note) h.note = note;
    else delete h.note;
    h.at = Date.now();
  });
  if (!done) return;
  for (const mark of marksOf(id)) {
    mark.classList.toggle('has-note', !!note);
    if (note) mark.title = note;
    else mark.removeAttribute('title');
  }
  flash('Note saved');
}

function removeHighlight(id: string) {
  if (!save((map) => delete map[id])) return;
  erase(id);
  flash('Highlight removed');
}

/* ── Note editor ───────────────────────────────────────── */
let dialog: HTMLDialogElement | null = null;
let submit: (note: string) => void = () => {};

function openNote(quote: string, value: string, onSave: (note: string) => void) {
  if (!dialog) {
    dialog = document.createElement('dialog');
    dialog.className = 'hl-note';
    dialog.setAttribute('aria-label', 'Note on highlighted text');
    dialog.innerHTML = `
      <blockquote></blockquote>
      <textarea rows="4" maxlength="${NOTE_MAX_CHARS}" aria-label="Your note"></textarea>
      <div class="hl-note-actions">
        <button type="button" data-save>Save</button>
        <button type="button" data-cancel>Cancel</button>
      </div>`;
    document.body.append(dialog);
    dialog.addEventListener('click', (e) => {
      const t = e.target as HTMLElement;
      if (t.closest('[data-save]')) {
        submit(dialog!.querySelector('textarea')!.value.trim());
        dialog!.close();
      } else if (t === dialog || t.closest('[data-cancel]')) dialog!.close();
    });
  }
  // textContent and .value only: a quote can come from an imported backup.
  dialog.querySelector('blockquote')!.textContent = quote;
  dialog.querySelector('textarea')!.value = value;
  submit = onSave;
  dialog.showModal();
  dialog.querySelector('textarea')!.focus();
}

/* ── Floating bar ──────────────────────────────────────── */
let bar: HTMLElement | null = null;
let picked: Picked | null = null;
let current: string | null = null;
const coarse = matchMedia('(pointer: coarse)');

function hideBar() {
  bar?.setAttribute('hidden', '');
  picked = null;
  current = null;
}

function showBar(rect: DOMRect, buttons: [label: string, act: () => void][]) {
  if (!bar) {
    bar = document.createElement('div');
    bar.className = 'hl-bar';
    bar.setAttribute('role', 'toolbar');
    bar.setAttribute('aria-label', 'Highlight');
    // Pressing a button must not clear the selection it acts on.
    bar.addEventListener('pointerdown', (e) => e.preventDefault());
    document.body.append(bar);
  }
  bar.replaceChildren(
    ...buttons.map(([label, act]) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.textContent = label;
      b.addEventListener('click', act);
      return b;
    }),
  );
  bar.hidden = false;
  // On touch screens the browser's own menu sits by the selection, so the bar docks at the bottom.
  bar.classList.toggle('docked', coarse.matches);
  if (coarse.matches) return;
  const { offsetWidth: w, offsetHeight: h } = bar;
  const header = parseFloat(getComputedStyle(document.documentElement).scrollPaddingTop) || 0;
  const above = rect.top - h - 8;
  const top = above > header ? above : rect.bottom + 8;
  const left = Math.min(
    Math.max(8, rect.left + rect.width / 2 - w / 2),
    document.documentElement.clientWidth - w - 8,
  );
  bar.style.top = `${top + scrollY}px`;
  bar.style.left = `${left + scrollX}px`;
}

function showForSelection(p: Picked) {
  picked = p;
  current = null;
  showBar(p.range.getBoundingClientRect(), [
    ['Highlight', () => act(false)],
    ['Add note', () => act(true)],
  ]);
}

function act(withNote: boolean) {
  const p = picked ?? readSelection();
  hideBar();
  if (!p) return;
  if (!withNote) return addHighlight(p);
  if (overlapsExisting(p.range)) return flash('That overlaps a highlight. Remove it first.');
  openNote(p.quote, '', (note) => addHighlight(p, note));
}

function showForMark(mark: HTMLElement) {
  const id = mark.dataset.hl!;
  const h = loadHighlights()[id];
  if (!h) return;
  picked = null;
  current = id;
  showBar(mark.getBoundingClientRect(), [
    [
      h.note ? 'Edit note' : 'Add note',
      () => {
        hideBar();
        openNote(h.quote, h.note ?? '', (note) => setNote(id, note));
      },
    ],
    [
      'Remove',
      () => {
        hideBar();
        removeHighlight(id);
      },
    ],
  ]);
}

/* ── Going to a highlight from the notes page ──────────── */
function revealFromHash() {
  if (!location.hash.startsWith('#hl-')) return;
  const [first, ...rest] = marksOf(location.hash.slice(4));
  if (!first) return;
  // It may be inside a closed answer, or in a code tab for another language.
  for (let d = first.closest('details'); d; d = d.parentElement?.closest('details') ?? null)
    d.open = true;
  const panel = first.closest<HTMLElement>('[role="tabpanel"][hidden]');
  if (panel) document.getElementById(panel.getAttribute('aria-labelledby') ?? '')?.click();
  first.scrollIntoView({ block: 'center' });
  for (const mark of [first, ...rest]) {
    mark.classList.add('hl-flash');
    setTimeout(() => mark.classList.remove('hl-flash'), 2000);
  }
}

/** For the h and n shortcuts. */
export function highlightSelection(withNote: boolean) {
  if (!roots.length || !topicId) return;
  const p = readSelection();
  if (!p) return flash('Select some text on the page first');
  picked = p;
  act(withNote);
}

if (roots.length && topicId) {
  drawAll();
  revealFromHash();
  addEventListener('hashchange', revealFromHash);

  let timer: ReturnType<typeof setTimeout> | undefined;
  document.addEventListener('selectionchange', () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      if (dialog?.open) return;
      const p = readSelection();
      if (p) showForSelection(p);
      else if (!current) hideBar();
    }, 150);
  });
  document.addEventListener('click', (e) => {
    const target = e.target as Element;
    if (target.closest('.hl-bar') || !getSelection()?.isCollapsed) return;
    // A highlight on a link or a question heading keeps that control's normal click.
    const mark = target.closest<HTMLElement>('mark.hl');
    if (mark && !target.closest('a, button, summary')) showForMark(mark);
    else hideBar();
  });
  document.addEventListener('keydown', (e) => e.key === 'Escape' && hideBar());
}
