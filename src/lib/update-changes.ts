/** What the last offline-copy update changed. The service worker reports it; scripts/offline.ts saves it. */
export interface UpdateChanges {
  version: string;
  at: number;
  /** built files that differ, including ones only the layout or a script name touched */
  files?: number | undefined;
  /** topics whose own source changed; missing when the older copy had no fingerprints to compare */
  topics?: { added: string[]; changed: string[]; removed: string[] } | null | undefined;
}
/** localStorage key (efg:last-update), read by the "what's new" page. */
export const LAST_UPDATE = 'last-update';
