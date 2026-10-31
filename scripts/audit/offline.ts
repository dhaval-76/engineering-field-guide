/**
 * Waits for the service worker to save the site, goes offline, then opens every page (scrolling so
 * lazy islands load). Fails if any page isn't served by the worker or any request fails.
 *   npm run build && npm run preview   (in another terminal)
 *   npm run audit:offline
 */
import { chromium } from 'playwright';
import { BASE, builtPages } from './pages';

const pages = builtPages();
const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 390, height: 844 } });
const page = await ctx.newPage();
await page.goto(BASE);
await page.waitForFunction(
  () => document.querySelector<HTMLElement>('[data-offline-indicator]')?.dataset.state === 'ready',
  null,
  { timeout: 180_000 },
);
await ctx.setOffline(true);

const failed: string[] = [];
const here = () => page.url().slice(BASE.length) || '/';
page.on('requestfailed', (r) => failed.push(`${here()} -> ${r.url()} ${r.failure()?.errorText}`));
page.on('response', (r) => {
  if (r.status() >= 400) failed.push(`${here()} -> ${r.url()} ${r.status()}`);
});
let bad = 0;
for (const p of pages) {
  const r = await page.goto(BASE + p, { waitUntil: 'load' });
  await page.evaluate(async () => {
    for (let y = 0; y < document.body.scrollHeight; y += 1500) {
      scrollTo(0, y);
      await new Promise((ok) => setTimeout(ok, 15));
    }
  });
  await page.waitForTimeout(100);
  if (!r || r.status() !== 200 || !r.fromServiceWorker()) {
    bad++;
    console.log('BAD', p || '/', r?.status());
  }
}
await browser.close();
console.log(
  `${pages.length} pages offline; ${bad} not from the service worker; ${failed.length} failed requests`,
);
if (failed.length) console.log(failed.slice(0, 20).join('\n'));
process.exit(bad || failed.length ? 1 : 0);
