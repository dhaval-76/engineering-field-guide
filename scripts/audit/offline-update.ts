/**
 * End-to-end test of the offline copy's update flow, in a fresh browser profile:
 *  1. an unknown page offline returns the 404 page with status 404;
 *  2. publishing a change to one file shows the update prompt with "1 changed file", and Reload
 *     switches to the new version, leaving one cache;
 *  3. deleting saved files shows them as missing and Repair restores them;
 *  4. Re-download everything finishes with every file saved;
 *  5. Check for updates with nothing new says so.
 * It edits one file in dist/ and regenerates dist/sw.js, then puts both back.
 *   npm run build && npm run preview   (in another terminal)
 *   npm run test:offline-update
 */
import { execSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { chromium } from 'playwright';
import { BASE } from './pages';

const failures: string[] = [];
const expect = (ok: boolean, what: string) => {
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${what}`);
  if (!ok) failures.push(what);
};
const rebuildWorker = () => execSync('npx tsx scripts/pwa/build-sw.ts', { stdio: 'ignore' });

const changed = 'dist/dsa/sliding-window/index.html';
const original = readFileSync(changed, 'utf8');
const profile = mkdtempSync(join(tmpdir(), 'efg-offline-'));
const ctx = await chromium.launchPersistentContext(profile);
const path = new URL(BASE).pathname;

try {
  const page = await ctx.newPage();
  page.on('pageerror', (e) => failures.push(`page error: ${e.message}`));
  const state = () => page.getAttribute('[data-offline-indicator]', 'data-state');
  const ready = () =>
    page.waitForFunction(
      () =>
        document.querySelector<HTMLElement>('[data-offline-indicator]')?.dataset.state === 'ready',
      null,
      { timeout: 180_000 },
    );
  const text = async (sel: string) => (await page.textContent(sel))?.trim() ?? '';

  await page.goto(`${BASE}offline/`);
  await ready();
  const v1 = await text('[data-f="version"]');

  await ctx.setOffline(true);
  const notFound = await page.goto(`${BASE}no-such-page/`);
  expect(
    notFound?.status() === 404 && !!notFound.fromServiceWorker(),
    'unknown page offline is a 404 from the saved copy',
  );
  await ctx.setOffline(false);

  writeFileSync(changed, original.replace('</body>', '<!-- update test --></body>'));
  rebuildWorker();
  await page.goto(`${BASE}offline/`);
  await page.waitForSelector('.update-toast', { timeout: 60_000 });
  expect(
    (await text('.update-toast span')).includes('1 changed file'),
    'update prompt counts 1 changed file',
  );
  await Promise.all([page.waitForEvent('load'), page.click('.update-toast [data-apply]')]);
  await ready();
  expect((await text('[data-f="version"]')) !== v1, 'Reload switches to the new version');
  expect((await page.evaluate(() => caches.keys())).length === 1, 'only the new cache is kept');
  const updated = await page.evaluate(
    async (p) =>
      (
        await (await caches.match(`${location.origin}${p}dsa/sliding-window/index.html`))?.text()
      )?.includes('<!-- update test -->'),
    path,
  );
  expect(!!updated, 'the changed file is saved');

  const files = await text('[data-f="files"]'); // "616 of 616"
  await page.evaluate(async (p) => {
    const cache = await caches.open((await caches.keys())[0]!);
    await cache.delete(`${location.origin}${p}glossary/index.html`);
    await cache.delete(`${location.origin}${p}dsa/index.html`);
  }, path);
  await page.reload();
  await page.waitForFunction(
    () => document.querySelector('[data-status]')?.textContent?.includes('missing'),
    null,
    { timeout: 30_000 },
  );
  expect(
    (await state()) === 'error' && (await page.isEnabled('[data-act="repair"]')),
    'deleted files show as missing, Repair enabled',
  );
  await page.click('[data-act="repair"]');
  await page.waitForFunction(
    (f) => document.querySelector('[data-f="files"]')?.textContent?.trim() === f,
    files,
    { timeout: 60_000 },
  );
  expect(true, 'Repair restores the missing files');

  await page.click('[data-act="resync"]');
  await ready();
  expect((await text('[data-f="files"]')) === files, 'Re-download everything saves every file');

  await page.click('[data-act="check"]');
  await page
    .waitForFunction(
      () => document.querySelector('[data-note]')?.textContent?.includes('latest'),
      null,
      { timeout: 15_000 },
    )
    .catch(() => {});
  expect(
    (await text('[data-note]')).includes('latest version'),
    'Check for updates reports the latest version',
  );
} finally {
  await ctx.close();
  writeFileSync(changed, original);
  rebuildWorker();
  rmSync(profile, { recursive: true, force: true });
}
console.log(failures.length ? `\n${failures.length} failed` : '\nall passed');
process.exit(failures.length ? 1 : 0);
