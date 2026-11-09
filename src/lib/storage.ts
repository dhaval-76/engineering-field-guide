/** Browser-only persistence. Every access is guarded: storage may be unavailable (private mode, blocked). */
const PREFIX = 'efg:';

export function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw == null ? fallback : (JSON.parse(raw) as T);
  } catch {
    return fallback;
  }
}

export function write(key: string, value: unknown): void {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    /* storage unavailable — state stays in memory for this page */
  }
}

/** Like write, but throws when the browser refuses (quota, blocked), for callers that must report it. */
export function writeStrict(key: string, value: unknown): void {
  localStorage.setItem(PREFIX + key, JSON.stringify(value));
}

export function remove(key: string): void {
  try {
    localStorage.removeItem(PREFIX + key);
  } catch {
    /* storage unavailable */
  }
}

/** Every key this site has stored, without the prefix. */
export function storedKeys(): string[] {
  try {
    return Object.keys(localStorage)
      .filter((k) => k.startsWith(PREFIX))
      .map((k) => k.slice(PREFIX.length));
  } catch {
    return [];
  }
}

export type ProgressMap = Record<string, number>; // topicId → completed-at epoch ms
/** Reading history, newest first: one entry per topic per day (see site.ts), up to HISTORY_LIMIT. */
export type RecentList = { id: string; title: string; at: number }[];
export const HISTORY_LIMIT = 200;

/** A private note per topic. */
export type NotesMap = Record<string, { text: string; at: number }>;
export const NOTE_MAX_CHARS = 20_000;

export const keys = {
  progress: 'progress',
  bookmarks: 'bookmarks',
  recent: 'recent',
  lang: 'lang',
  theme: 'theme',
  focus: 'focus',
  wrap: 'wrap',
  /** quiz answers per question, for /review/ (see lib/review.ts) */
  quiz: 'quiz',
  notes: 'notes',
  highlights: 'highlights',
  /** when progress was last exported, and until when the backup reminder is snoozed (epoch ms) */
  lastExport: 'last-export',
  backupSnooze: 'backup-snooze',
} as const;

/** Notify other islands on the same page that some stored state changed. */
export function broadcast(key: string) {
  window.dispatchEvent(new CustomEvent('efg:storage', { detail: key }));
}
