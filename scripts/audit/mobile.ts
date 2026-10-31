/**
 * Opens every built page at phone width and reports horizontal page scroll (must be none), tap
 * targets under 24px and text under 12px.
 *   npm run build && npm run preview   (in another terminal)
 *   npm run audit:mobile
 */
import { chromium } from 'playwright';
import { BASE, builtPages } from './pages';

const pages = builtPages();
const browser = await chromium.launch();
const ctx = await browser.newContext({
  viewport: { width: 360, height: 780 },
  isMobile: true,
  hasTouch: true,
});
const page = await ctx.newPage();
// tsx keeps function names by wrapping them in __name(), which doesn't exist inside the page.
await page.addInitScript('globalThis.__name = (f) => f');
const found: Record<string, string[]> = {};
const overflow: string[] = [];

for (const path of pages) {
  await page.goto(BASE + path);
  const r = await page.evaluate(() => {
    const out = {
      overflow: document.documentElement.scrollWidth > innerWidth + 1,
      small: [] as string[],
      tiny: [] as string[],
    };
    const desc = (el: Element) =>
      el.tagName.toLowerCase() +
      (typeof el.className === 'string' && el.className.trim()
        ? `.${el.className.trim().split(/\s+/)[0]}`
        : '');
    for (const el of document.querySelectorAll('a, button, summary, [role=tab], input, select')) {
      const s = getComputedStyle(el);
      if (s.display === 'none' || s.visibility === 'hidden') continue;
      const b = el.getBoundingClientRect();
      if (!b.width || !b.height) continue;
      // Links inside running text are exempt, as are controls in closed dialogs.
      if (el.tagName === 'A' && el.closest('p, li, td, dd, figcaption, .callout')) continue;
      if (el.closest('dialog:not([open])')) continue;
      if (b.height < 24 || b.width < 24) out.small.push(desc(el));
    }
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    const seen = new Set<Element>();
    while (walker.nextNode()) {
      const el = walker.currentNode.parentElement;
      if (!el || seen.has(el) || !walker.currentNode.textContent?.trim() || el.closest('svg'))
        continue;
      seen.add(el);
      const s = getComputedStyle(el);
      if (s.display !== 'none' && parseFloat(s.fontSize) < 12 && el.getClientRects().length)
        out.tiny.push(`${desc(el)} ${s.fontSize}`);
    }
    return out;
  });
  if (r.overflow) overflow.push(path || '/');
  for (const k of new Set(r.small.map((x) => `small target ${x}`))) (found[k] ??= []).push(path);
  for (const k of new Set(r.tiny.map((x) => `small text ${x}`))) (found[k] ??= []).push(path);
}
await browser.close();

console.log(
  `${pages.length} pages; horizontal scroll on ${overflow.length}`,
  overflow.slice(0, 10),
);
for (const [k, v] of Object.entries(found).sort((a, b) => b[1].length - a[1].length))
  console.log(String(v.length).padStart(4), k, '  e.g.', v[0] || '/');
process.exit(overflow.length ? 1 : 0);
