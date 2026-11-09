/**
 * Backup, restore and device-to-device copy of what this browser stores. Nothing here leaves the
 * device unless the reader downloads a file or copies a code. Every incoming file or code is
 * validated field by field, and only known fields are kept.
 */
import { BOX_DAYS, type QuizHistory } from './review';
import {
  broadcast,
  HISTORY_LIMIT,
  keys,
  NOTE_MAX_CHARS,
  read,
  remove,
  storedKeys,
  writeStrict,
  type NotesMap,
  type ProgressMap,
  type RecentList,
} from './storage';

export const APP_ID = 'engineering-field-guide';
export const SCHEMA_VERSION = 1;
const SYNC_PREFIX = 'EFG1.';
/** A real backup is well under 1 MB; anything far bigger is not one of ours. */
export const MAX_IMPORT_BYTES = 5_000_000;
const MAX_ENTRIES = 5000;
const DAY = 24 * 60 * 60 * 1000;

export interface Preferences {
  lang?: string | undefined;
  theme?: 'light' | 'dark' | undefined;
  focus?: boolean | undefined;
  wrap?: boolean | undefined;
}
export interface UserData {
  progress: ProgressMap;
  bookmarks: string[];
  recent: RecentList;
  quiz: QuizHistory;
  notes: NotesMap;
  preferences: Preferences;
}
export type SectionId = keyof UserData;
export interface Bundle {
  app: string;
  schemaVersion: number;
  exportedAt: string;
  data: Partial<UserData>;
}

export const SECTIONS: { id: SectionId; label: string }[] = [
  { id: 'progress', label: 'Completed topics' },
  { id: 'bookmarks', label: 'Saved topics' },
  { id: 'recent', label: 'Reading history' },
  { id: 'quiz', label: 'Quiz answers' },
  { id: 'notes', label: 'Topic notes' },
  { id: 'preferences', label: 'Preferences' },
];
const PREFERENCE_KEYS = ['lang', 'theme', 'focus', 'wrap'] as const;
const STORAGE_KEYS: Record<SectionId, readonly string[]> = {
  progress: [keys.progress],
  bookmarks: [keys.bookmarks],
  recent: [keys.recent],
  quiz: [keys.quiz],
  notes: [keys.notes],
  preferences: PREFERENCE_KEYS,
};

export function loadAll(): UserData {
  const preferences: Preferences = {};
  for (const k of PREFERENCE_KEYS) {
    const v = read<unknown>(k, null);
    if (v !== null) Object.assign(preferences, { [k]: v });
  }
  return {
    progress: read<ProgressMap>(keys.progress, {}),
    bookmarks: read<string[]>(keys.bookmarks, []),
    recent: read<RecentList>(keys.recent, []),
    quiz: read<QuizHistory>(keys.quiz, {}),
    notes: read<NotesMap>(keys.notes, {}),
    preferences,
  };
}

export function buildBundle(data: UserData, sections: Iterable<SectionId>): Bundle {
  const picked: Partial<UserData> = {};
  for (const id of sections) Object.assign(picked, { [id]: data[id] });
  return {
    app: APP_ID,
    schemaVersion: SCHEMA_VERSION,
    exportedAt: new Date().toISOString(),
    data: picked,
  };
}

/* ── Validation ────────────────────────────────────────── */

/** A problem with a backup, worded for the reader. */
export class ImportError extends Error {}

const TOPIC_ID = /^[a-z0-9-]+\/[a-z0-9-]+$/;
const QUESTION_ID = /^[a-z0-9-]+\/[a-z0-9-]+#[a-z0-9]+$/;
const isObject = (x: unknown): x is Record<string, unknown> =>
  typeof x === 'object' && x !== null && !Array.isArray(x);
/** A past moment, allowing a day for a device whose clock runs fast. */
const isStamp = (x: unknown): x is number =>
  Number.isInteger(x) && (x as number) > 0 && (x as number) <= Date.now() + DAY;
const isCount = (x: unknown): x is number => Number.isInteger(x) && (x as number) >= 0;

function fail(label: string, why: string): never {
  throw new ImportError(`The “${label}” part of this backup is damaged: ${why}.`);
}
function entriesOf(value: unknown, label: string): [string, unknown][] {
  if (!isObject(value)) fail(label, 'it has the wrong shape');
  const entries = Object.entries(value);
  if (entries.length > MAX_ENTRIES) fail(label, 'it has too many entries');
  return entries;
}
function listOf(value: unknown, label: string): unknown[] {
  if (!Array.isArray(value)) fail(label, 'it has the wrong shape');
  if (value.length > MAX_ENTRIES) fail(label, 'it has too many entries');
  return value;
}

const validators: { [K in SectionId]: (value: unknown, label: string) => UserData[K] } = {
  progress(value, label) {
    return Object.fromEntries(
      entriesOf(value, label).map(([id, at]) => {
        if (!TOPIC_ID.test(id)) fail(label, `“${id.slice(0, 60)}” is not a topic`);
        if (!isStamp(at)) fail(label, `the date for ${id} is invalid`);
        return [id, at];
      }),
    );
  },
  bookmarks(value, label) {
    return listOf(value, label).map((id) => {
      if (typeof id !== 'string' || !TOPIC_ID.test(id)) fail(label, 'an entry is not a topic');
      return id;
    });
  },
  recent(value, label) {
    return listOf(value, label).map((r) => {
      if (!isObject(r)) fail(label, 'an entry has the wrong shape');
      const { id, title, at } = r;
      if (typeof id !== 'string' || !TOPIC_ID.test(id)) fail(label, 'an entry is not a topic');
      if (typeof title !== 'string' || title.length > 300)
        fail(label, `the title of ${id} is invalid`);
      if (!isStamp(at)) fail(label, `the date for ${id} is invalid`);
      return { id, title, at };
    });
  },
  quiz(value, label) {
    return Object.fromEntries(
      entriesOf(value, label).map(([id, r]) => {
        if (!QUESTION_ID.test(id) || !isObject(r)) fail(label, 'an entry is not a quiz answer');
        const { box, due, last, right, wrong } = r;
        const validBox = isCount(box) && box < BOX_DAYS.length;
        if (!validBox || !isCount(due) || !isStamp(last) || !isCount(right) || !isCount(wrong))
          fail(label, `the record for ${id} is invalid`);
        return [id, { box, due, last, right, wrong }];
      }),
    );
  },
  notes(value, label) {
    return Object.fromEntries(
      entriesOf(value, label).map(([id, n]) => {
        if (!TOPIC_ID.test(id) || !isObject(n)) fail(label, 'an entry is not a topic note');
        const { text, at } = n;
        if (typeof text !== 'string' || !text || text.length > NOTE_MAX_CHARS)
          fail(label, `the note for ${id} is invalid`);
        if (!isStamp(at)) fail(label, `the date for ${id} is invalid`);
        return [id, { text, at }];
      }),
    );
  },
  preferences(value, label) {
    if (!isObject(value)) fail(label, 'it has the wrong shape');
    const { lang, theme, focus, wrap } = value;
    const out: Preferences = {};
    if (lang !== undefined) {
      if (typeof lang !== 'string' || !/^[a-z]{1,20}$/.test(lang))
        fail(label, 'the language is invalid');
      out.lang = lang;
    }
    if (theme !== undefined) {
      if (theme !== 'light' && theme !== 'dark') fail(label, 'the theme is invalid');
      out.theme = theme;
    }
    for (const [k, v] of [
      ['focus', focus],
      ['wrap', wrap],
    ] as const) {
      if (v === undefined) continue;
      if (typeof v !== 'boolean') fail(label, `the ${k} setting is invalid`);
      out[k] = v;
    }
    return out;
  },
};

export interface ParsedBackup {
  bundle: Bundle;
  /** sections in the file this version of the site doesn't know */
  ignored: string[];
}

export function parseBackup(text: string): ParsedBackup {
  if (text.length > MAX_IMPORT_BYTES)
    throw new ImportError('This file is too large to be a backup.');
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    throw new ImportError('This is not a Field Guide backup: it is not valid JSON.');
  }
  if (!isObject(raw) || raw.app !== APP_ID)
    throw new ImportError('This is not a Field Guide backup.');
  const version = raw.schemaVersion;
  if (!Number.isInteger(version) || (version as number) < 1)
    throw new ImportError('This backup has an unknown format version, so it cannot be read.');
  if ((version as number) > SCHEMA_VERSION)
    throw new ImportError(
      `This backup was made by a newer version of the guide (format ${version}; this copy reads up to ${SCHEMA_VERSION}). Update the guide on this device from the Offline page, then try again.`,
    );
  if (!isObject(raw.data)) throw new ImportError('This backup has no data in it.');
  if (typeof raw.exportedAt !== 'string' || Number.isNaN(Date.parse(raw.exportedAt)))
    throw new ImportError('This backup has no valid export date.');

  const data: Partial<UserData> = {};
  for (const { id, label } of SECTIONS)
    if (raw.data[id] !== undefined)
      Object.assign(data, { [id]: validators[id](raw.data[id], label) });
  if (!Object.keys(data).length) throw new ImportError('This backup has no data in it.');
  const known = new Set<string>(SECTIONS.map((s) => s.id));
  return {
    bundle: { app: APP_ID, schemaVersion: SCHEMA_VERSION, exportedAt: raw.exportedAt, data },
    ignored: Object.keys(raw.data).filter((k) => !known.has(k)),
  };
}

/* ── Merge, replace and the preview ────────────────────── */

export type ImportMode = 'merge' | 'replace';

/** Later of two records by a timestamp; the record on this device wins a tie. */
function newest<T>(mine: Record<string, T>, theirs: Record<string, T>, at: (r: T) => number) {
  const out = new Map(Object.entries(mine));
  for (const [id, r] of Object.entries(theirs)) {
    const have = out.get(id);
    if (have === undefined || at(r) > at(have)) out.set(id, r);
  }
  return Object.fromEntries(out);
}

const dayKey = (r: { id: string; at: number }) => `${r.id}@${new Date(r.at).toDateString()}`;
const tidyHistory = (list: RecentList) =>
  [...list].sort((a, b) => b.at - a.at).slice(0, HISTORY_LIMIT);

function mergeHistory(mine: RecentList, theirs: RecentList): RecentList {
  const byDay = new Map<string, RecentList[number]>();
  for (const r of [...mine, ...theirs]) {
    const have = byDay.get(dayKey(r));
    if (!have || r.at > have.at) byDay.set(dayKey(r), r);
  }
  return tidyHistory([...byDay.values()]);
}

function combine<K extends SectionId>(id: K, mine: UserData[K], theirs: UserData[K]): UserData[K];
function combine(id: SectionId, mine: unknown, theirs: unknown): unknown {
  switch (id) {
    case 'progress':
      return newest(mine as ProgressMap, theirs as ProgressMap, (at) => at);
    case 'bookmarks':
      return [...new Set([...(mine as string[]), ...(theirs as string[])])];
    case 'recent':
      return mergeHistory(mine as RecentList, theirs as RecentList);
    case 'quiz':
      return newest(mine as QuizHistory, theirs as QuizHistory, (r) => r.last);
    case 'notes':
      return newest(mine as NotesMap, theirs as NotesMap, (n) => n.at);
    case 'preferences':
      // No timestamps to compare, and preferences are often per device: only fill in what's unset.
      return { ...(theirs as Preferences), ...(mine as Preferences) };
  }
}

/** A section as key → value, so any two versions of it can be compared the same way. */
function comparable(id: SectionId, value: UserData[SectionId]): Map<string, string> {
  if (id === 'bookmarks') return new Map((value as string[]).map((b) => [b, '']));
  if (id === 'recent') return new Map((value as RecentList).map((r) => [dayKey(r), String(r.at)]));
  return new Map(Object.entries(value).map(([k, v]) => [k, JSON.stringify(v)]));
}

export interface PlanRow {
  id: SectionId;
  label: string;
  before: number;
  after: number;
  added: number;
  updated: number;
  removed: number;
}
export interface ImportPlan {
  next: Partial<UserData>;
  rows: PlanRow[];
}

/** What importing would do. Changes nothing, so the reader can look before anything is written. */
export function planImport(
  current: UserData,
  bundle: Bundle,
  mode: ImportMode,
  chosen: ReadonlySet<SectionId>,
): ImportPlan {
  const next: Partial<UserData> = {};
  const rows: PlanRow[] = [];
  for (const { id, label } of SECTIONS) {
    const incoming = bundle.data[id];
    if (incoming === undefined || !chosen.has(id)) continue;
    const result =
      mode === 'merge'
        ? combine(id, current[id], incoming)
        : id === 'recent'
          ? tidyHistory(incoming as RecentList)
          : incoming;
    Object.assign(next, { [id]: result });
    const before = comparable(id, current[id]);
    const after = comparable(id, result);
    let added = 0;
    let updated = 0;
    for (const [k, v] of after) {
      if (!before.has(k)) added++;
      else if (before.get(k) !== v) updated++;
    }
    const removed = [...before.keys()].filter((k) => !after.has(k)).length;
    rows.push({ id, label, before: before.size, after: after.size, added, updated, removed });
  }
  return { next, rows };
}

function writeSection(id: SectionId, value: UserData[SectionId]) {
  if (id !== 'preferences') return writeStrict(STORAGE_KEYS[id][0]!, value);
  for (const k of PREFERENCE_KEYS) {
    const v = (value as Preferences)[k];
    if (v === undefined) remove(k);
    else writeStrict(k, v);
  }
}

/** Writes the plan, or puts everything back and throws if the browser refuses part of it. */
export function applyImport(plan: ImportPlan) {
  const ids = plan.rows.map((r) => r.id);
  const backup = ids.flatMap((id) =>
    STORAGE_KEYS[id].map((k) => [k, read<unknown>(k, undefined)] as const),
  );
  try {
    for (const id of ids) writeSection(id, plan.next[id]!);
  } catch {
    for (const [k, v] of backup) {
      remove(k);
      if (v !== undefined) writeStrict(k, v);
    }
    throw new ImportError('This browser is out of space or blocking storage. Nothing was changed.');
  }
  ids.forEach((id) => STORAGE_KEYS[id].forEach(broadcast));
}

export function deleteSection(id: SectionId) {
  for (const k of STORAGE_KEYS[id]) {
    remove(k);
    broadcast(k);
  }
}

/** Removes every key this site stored in this browser. The offline copy of the site is separate. */
export function deleteEverything() {
  for (const k of storedKeys()) {
    remove(k);
    broadcast(k);
  }
}

/* ── Sync codes: a backup as compressed text ───────────── */

async function drain(stream: ReadableStream<Uint8Array>): Promise<Uint8Array<ArrayBuffer>> {
  const reader = stream.getReader();
  const parts: Uint8Array[] = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.length;
    if (total > MAX_IMPORT_BYTES) {
      await reader.cancel();
      throw new ImportError('This code expands to more data than a backup can hold.');
    }
    parts.push(value);
  }
  const out = new Uint8Array(total);
  let at = 0;
  for (const p of parts) {
    out.set(p, at);
    at += p.length;
  }
  return out;
}

export async function encodeSyncCode(bundle: Bundle): Promise<string> {
  const json = new TextEncoder().encode(JSON.stringify(bundle));
  const packed = await drain(
    new Blob([json]).stream().pipeThrough(new CompressionStream('deflate-raw')),
  );
  let binary = '';
  for (const byte of packed) binary += String.fromCharCode(byte);
  return SYNC_PREFIX + btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function decodeSyncCode(code: string): Promise<string> {
  const body = code.slice(SYNC_PREFIX.length).replace(/-/g, '+').replace(/_/g, '/');
  try {
    const bytes = Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
    const out = await drain(
      new Blob([bytes]).stream().pipeThrough(new DecompressionStream('deflate-raw')),
    );
    return new TextDecoder().decode(out);
  } catch (err) {
    if (err instanceof ImportError) throw err;
    throw new ImportError('This code is damaged or incomplete. Copy it again and paste all of it.');
  }
}

export const syncLink = (pageUrl: string, code: string) => `${pageUrl}#sync=${code}`;

/** Reads a pasted code, a pasted sync link or the text of a backup file. */
export async function readBackup(input: string): Promise<ParsedBackup> {
  const text = input.trim();
  const code = text.includes('#sync=') ? text.slice(text.indexOf('#sync=') + 6) : text;
  return parseBackup(
    code.startsWith(SYNC_PREFIX) ? await decodeSyncCode(code.replace(/\s/g, '')) : text,
  );
}
