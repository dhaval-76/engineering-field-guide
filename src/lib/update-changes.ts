/** What the last offline-copy update changed. The service worker reports it; scripts/offline.ts saves it. */
export interface UpdateChanges {
  version: string;
  at: number;
  /** built file paths, e.g. `dsa/sliding-window/index.html` */
  added: string[];
  changed: string[];
  removed: string[];
}
/** localStorage key (efg:last-update), read by the "what's new" page. */
export const LAST_UPDATE = 'last-update';
