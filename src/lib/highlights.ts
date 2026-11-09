/** Text highlights, each with an optional note. Stored in this browser under `efg:highlights`. */
import { broadcast, keys, read, writeStrict } from './storage';

export interface Highlight {
  topic: string;
  /** The exact text. Found again by searching, so it survives edits elsewhere on the page. */
  quote: string;
  /** The text just before and after, to pick the right place when the quote appears twice. */
  prefix: string;
  suffix: string;
  note?: string | undefined;
  /** Created or last edited (epoch ms). */
  at: number;
}
export type HighlightMap = Record<string, Highlight>;

export const QUOTE_MAX_CHARS = 5000;
export const CONTEXT_CHARS = 32;

export const loadHighlights = () => read<HighlightMap>(keys.highlights, {});

/** Throws if the browser refuses, so callers can say the highlight wasn't saved. */
export function saveHighlights(map: HighlightMap) {
  writeStrict(keys.highlights, map);
  broadcast(keys.highlights);
}

export const newHighlightId = () =>
  Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
