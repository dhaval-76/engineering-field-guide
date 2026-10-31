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

export type ProgressMap = Record<string, number>; // topicId → completed-at epoch ms
export type RecentList = { id: string; title: string; at: number }[];

export const keys = {
  progress: 'progress',
  bookmarks: 'bookmarks',
  recent: 'recent',
  lang: 'lang',
  theme: 'theme',
  focus: 'focus',
  wrap: 'wrap',
} as const;

/** Notify other islands on the same page that some stored state changed. */
export function broadcast(key: string) {
  window.dispatchEvent(new CustomEvent('efg:storage', { detail: key }));
}
