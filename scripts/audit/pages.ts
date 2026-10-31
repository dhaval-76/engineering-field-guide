/** Shared by the browser audits: the preview URL and every page in the built site. */
import { readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

/** Run `npm run build && npm run preview` first. Override with PREVIEW_URL for another host or port. */
export const BASE = process.env.PREVIEW_URL ?? 'http://localhost:4321/engineering-field-guide/';

/** Paths relative to BASE, e.g. '' for home and 'dsa/sliding-window/' for a topic. */
export function builtPages(dist = 'dist'): string[] {
  const pages: string[] = [];
  (function walk(dir: string, rel: string) {
    for (const f of readdirSync(dir)) {
      const p = join(dir, f);
      if (statSync(p).isDirectory()) {
        if (!['_astro', 'pagefind', 'og', 'icons'].includes(f)) walk(p, `${rel}${f}/`);
      } else if (f === 'index.html') pages.push(rel);
    }
  })(dist, '');
  return pages.sort();
}
